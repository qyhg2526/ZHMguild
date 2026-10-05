package cn.zhm.guild.listener;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.GuildWar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.Locale;

/**
 * 公会战相关事件: 淘汰判定、观看点复活、禁止丢弃物品与飞行、禁用指令。
 */
public class GuildWarListener implements Listener {

    private final ZHMguildPlugin plugin;

    public GuildWarListener(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    /** 战斗阶段死亡即被淘汰。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GuildWar war = plugin.getGuildWarManager().getWar(player.getUniqueId());
        if (war == null || war.getState() != GuildWar.State.RUNNING || !war.isAlive(player.getUniqueId())) {
            return;
        }
        if (plugin.getConfigManager().guildWarKeepInventory()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        plugin.getGuildWarManager().eliminate(player, player.getKiller());
    }

    /** 淘汰后复活在观看点(未设置则回到原位)。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GuildWar war = plugin.getGuildWarManager().getWar(player.getUniqueId());
        if (war == null) {
            return;
        }
        Location spectate = war.getArena().getSpectateSpawn();
        if (spectate != null && spectate.getWorld() != null) {
            event.setRespawnLocation(spectate);
            plugin.getMessages().send(player, "war.spectating");
            return;
        }
        Location back = war.getReturnLocation(player.getUniqueId());
        if (back != null) {
            event.setRespawnLocation(back);
        }
    }

    /** 公会战准备与参与中无法丢弃物品。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().guildWarBlockDrop()) {
            return;
        }
        if (plugin.getGuildWarManager().isInWar(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessages().send(event.getPlayer(), "war.drop-blocked");
        }
    }

    /** 公会战期间禁止飞行。 */
    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!plugin.getConfigManager().guildWarDisableFly() || !event.isFlying()) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getGuildWarManager().isInWar(player.getUniqueId())) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    /** 公会战期间禁用配置中指定的指令(管理员豁免)。 */
    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGuildWarManager().isInWar(player.getUniqueId())
                || player.hasPermission("zhmguild.admin")) {
            return;
        }
        String message = event.getMessage().toLowerCase(Locale.ROOT).trim();
        String label = message.split(" ")[0];
        for (String blocked : plugin.getConfigManager().guildWarBlockedCommands()) {
            if (blocked == null || blocked.isBlank()) {
                continue;
            }
            String target = blocked.toLowerCase(Locale.ROOT).trim();
            if (!target.startsWith("/")) {
                target = "/" + target;
            }
            if (label.equals(target)) {
                event.setCancelled(true);
                plugin.getMessages().send(player, "war.command-blocked", cn.zhm.guild.util.Placeholders.of()
                        .put("command", target));
                return;
            }
        }
    }
}
