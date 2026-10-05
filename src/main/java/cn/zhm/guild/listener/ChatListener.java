package cn.zhm.guild.listener;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 聊天监听: 处理聊天栏输入、公会聊天频道与快捷前缀。
 */
public class ChatListener implements Listener {

    private final ZHMguildPlugin plugin;

    public ChatListener(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 1. 聊天栏输入流程
        if (plugin.getChatInputManager().isWaiting(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getChatInputManager().handle(player, message));
            return;
        }

        // 2. 公会聊天
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            return;
        }
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        if (member == null) {
            return;
        }
        String prefix = plugin.getConfigManager().chatQuickPrefix();
        boolean quick = prefix != null && !prefix.isEmpty() && message.startsWith(prefix);
        if (!quick && !member.isChatToggled()) {
            return;
        }
        String content = quick ? message.substring(prefix.length()) : message;
        if (content.isBlank()) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getGuildChatManager().send(player, guild, member, content));
    }
}
