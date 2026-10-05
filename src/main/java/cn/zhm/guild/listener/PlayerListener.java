package cn.zhm.guild.listener;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.util.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 玩家相关事件监听。
 */
public class PlayerListener implements Listener {

    private final ZHMguildPlugin plugin;

    public PlayerListener(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
            if (guild != null) {
                if (plugin.getConfigManager().joinReminder()) {
                    plugin.getMessages().send(player, "reminder.has-guild", Placeholders.of()
                            .put("guild", guild.getName())
                            .put("level", guild.getLevel()));
                }
                if (guild.getApplicationCount() > 0 && plugin.getGuildManager().getMember(player.getUniqueId()) != null
                        && plugin.getGuildManager().getMember(player.getUniqueId()).canManage()) {
                    plugin.getMessages().send(player, "reminder.pending-applications", Placeholders.of()
                            .put("count", guild.getApplicationCount()));
                }
            } else if (plugin.getConfigManager().joinReminderNoGuild()) {
                plugin.getMessages().send(player, "reminder.no-guild");
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getTeleportManager().cancel(uuid, false);
        plugin.getChatInputManager().cancel(uuid);
        plugin.getConfirmManager().cancel(uuid);
        plugin.getGuildWarManager().handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !plugin.getTeleportManager().isTeleporting(event.getPlayer().getUniqueId())) {
            return;
        }
        plugin.getTeleportManager().checkMove(event.getPlayer(), event.getTo());
    }

    /** 同公会成员之间的 PVP 保护, 公会战期间的伤害由公会战规则优先判定。 */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player damager = resolveDamager(event);
        if (damager == null) {
            return;
        }
        switch (plugin.getGuildWarManager().damageRule(damager, victim)) {
            case ALLOW -> {
                return;
            }
            case DENY -> {
                event.setCancelled(true);
                return;
            }
            default -> {
                // 继续走普通规则
            }
        }
        Guild victimGuild = plugin.getGuildManager().getGuildByPlayer(victim.getUniqueId());
        if (victimGuild == null) {
            return;
        }
        if (victimGuild != plugin.getGuildManager().getGuildByPlayer(damager.getUniqueId())) {
            return;
        }
        if (!victimGuild.isPvp()) {
            event.setCancelled(true);
        }
    }

    private Player resolveDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
