package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.util.Placeholders;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 聊天栏输入管理器: 让玩家在聊天栏输入文本作为参数。
 */
public class ChatInputManager {

    private final ZHMguildPlugin plugin;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public ChatInputManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    private record PendingInput(Consumer<String> callback, long expireAt) {
    }

    /** 请求玩家在聊天栏输入内容。 */
    public void request(Player player, String messagePath, Placeholders placeholders, int seconds, Consumer<String> callback) {
        pending.put(player.getUniqueId(), new PendingInput(callback, System.currentTimeMillis() + seconds * 1000L));
        plugin.getMessages().send(player, messagePath, placeholders);
    }

    public void request(Player player, String messagePath, int seconds, Consumer<String> callback) {
        request(player, messagePath, Placeholders.of(), seconds, callback);
    }

    public boolean isWaiting(UUID uuid) {
        PendingInput input = pending.get(uuid);
        if (input == null) {
            return false;
        }
        if (System.currentTimeMillis() > input.expireAt()) {
            pending.remove(uuid);
            return false;
        }
        return true;
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    /**
     * 处理一条聊天消息。
     *
     * @return true 表示该消息已被输入流程消费, 不应作为普通聊天发送
     */
    public boolean handle(Player player, String message) {
        PendingInput input = pending.remove(player.getUniqueId());
        if (input == null) {
            return false;
        }
        if (System.currentTimeMillis() > input.expireAt()) {
            plugin.getMessages().send(player, "common.input-expired");
            return true;
        }
        String text = message == null ? "" : message.trim();
        if (text.equalsIgnoreCase("cancel") || text.equals("取消") || text.equalsIgnoreCase("q")) {
            plugin.getMessages().send(player, "common.input-cancelled");
            return true;
        }
        try {
            input.callback().accept(text);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("处理聊天输入时出错: " + throwable.getMessage());
        }
        return true;
    }
}
