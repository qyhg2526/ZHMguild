package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公会聊天频道。
 */
public class GuildChatManager {

    private final ZHMguildPlugin plugin;
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    public GuildChatManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSpy(UUID uuid) {
        return spies.contains(uuid);
    }

    public boolean toggleSpy(Player player) {
        if (spies.remove(player.getUniqueId())) {
            plugin.getMessages().send(player, "chat.spy-off");
            return false;
        }
        spies.add(player.getUniqueId());
        plugin.getMessages().send(player, "chat.spy-on");
        return true;
    }

    public void removeSpy(UUID uuid) {
        spies.remove(uuid);
    }

    /** 发送一条公会聊天消息。 */
    public void send(Player player, Guild guild, GuildMember member, String message) {
        if (player == null || guild == null || member == null) {
            return;
        }
        if (message == null || message.isBlank()) {
            return;
        }
        String format = plugin.getConfigManager().chatFormat();
        if (plugin.getConfigManager().chatShowTag()) {
            format = "{tag} " + format;
        }
        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .putRaw("tag", plugin.getGuildManager().renderTag(guild))
                .putRaw("role", plugin.getConfigManager().roleDisplay(member.getRole()))
                .put("player", player.getName())
                .put("message", Text.escape(message.trim()));

        Component component = plugin.getMessages().parse(placeholders.apply(format), player);
        for (GuildMember guildMember : guild.getMembers()) {
            Player online = Bukkit.getPlayer(guildMember.getUuid());
            if (online != null) {
                online.sendMessage(component);
            }
        }
        // 监听者
        if (plugin.getConfigManager().chatSpyEnabled()) {
            for (UUID uuid : spies) {
                if (guild.isMember(uuid)) {
                    continue;
                }
                Player spy = Bukkit.getPlayer(uuid);
                if (spy != null) {
                    plugin.getMessages().send(spy, "chat.spy-format", Placeholders.of()
                            .put("guild", guild.getName())
                            .put("player", player.getName())
                            .put("message", Text.escape(message.trim())));
                }
            }
        }
    }
}
