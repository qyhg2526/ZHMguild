package cn.zhm.guild.listener;

import cn.zhm.guild.ZHMguildPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 聊天监听: 仅用于把聊天栏内容作为命令参数输入(创建公会、修改公告/名称等)。
 */
public class ChatListener implements Listener {

    private final ZHMguildPlugin plugin;

    public ChatListener(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().isWaiting(player.getUniqueId())) {
            return;
        }
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getChatInputManager().handle(player, message));
    }
}
