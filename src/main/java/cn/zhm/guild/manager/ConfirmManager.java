package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.util.Placeholders;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 危险操作二次确认管理器。
 */
public class ConfirmManager {

    private final ZHMguildPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ConfirmManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    private record Pending(Runnable action, long expireAt) {
    }

    public void request(Player player, String messagePath, Placeholders placeholders, Runnable action) {
        pending.put(player.getUniqueId(), new Pending(action, System.currentTimeMillis() + 30_000L));
        plugin.getMessages().send(player, messagePath, placeholders);
    }

    public void request(Player player, String messagePath, Runnable action) {
        request(player, messagePath, Placeholders.of(), action);
    }

    public boolean hasPending(UUID uuid) {
        Pending value = pending.get(uuid);
        if (value == null) {
            return false;
        }
        if (System.currentTimeMillis() > value.expireAt()) {
            pending.remove(uuid);
            return false;
        }
        return true;
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    /** 执行待确认操作。 */
    public boolean confirm(Player player) {
        Pending value = pending.remove(player.getUniqueId());
        if (value == null || System.currentTimeMillis() > value.expireAt()) {
            plugin.getMessages().send(player, "common.no-pending-confirm");
            return false;
        }
        try {
            value.action().run();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("执行确认操作时出错: " + throwable.getMessage());
        }
        return true;
    }
}
