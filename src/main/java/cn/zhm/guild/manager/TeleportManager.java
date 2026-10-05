package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.util.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带读条的公会主城传送。
 */
public class TeleportManager {

    private final ZHMguildPlugin plugin;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TeleportManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    private static final class PendingTeleport {
        private final Location origin;
        private final Location target;
        private final long endTime;
        private BukkitTask task;

        private PendingTeleport(Location origin, Location target, long endTime) {
            this.origin = origin;
            this.target = target;
            this.endTime = endTime;
        }
    }

    public boolean isTeleporting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public long cooldownLeft(UUID uuid) {
        long until = cooldowns.getOrDefault(uuid, 0L);
        long left = until - System.currentTimeMillis();
        return Math.max(0L, left);
    }

    public void setCooldown(UUID uuid, int seconds) {
        if (seconds > 0) {
            cooldowns.put(uuid, System.currentTimeMillis() + seconds * 1000L);
        }
    }

    /** 开始传送到指定位置。 */
    public void teleport(Player player, Location target) {
        if (player == null || target == null) {
            return;
        }
        int delay = plugin.getConfigManager().homeTeleportDelay();
        if (delay <= 0) {
            player.teleport(target);
            plugin.getMessages().send(player, "home.teleported");
            return;
        }
        cancel(player.getUniqueId(), false);
        PendingTeleport teleport = new PendingTeleport(player.getLocation().clone(), target,
                System.currentTimeMillis() + delay * 1000L);
        pending.put(player.getUniqueId(), teleport);
        plugin.getMessages().send(player, "home.teleporting", Placeholders.of().put("time", delay));

        teleport.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel(player.getUniqueId(), false);
                return;
            }
            PendingTeleport current = pending.get(player.getUniqueId());
            if (current == null) {
                return;
            }
            long left = current.endTime - System.currentTimeMillis();
            if (left <= 0) {
                pending.remove(player.getUniqueId());
                if (current.task != null) {
                    current.task.cancel();
                }
                player.teleport(current.target);
                plugin.getMessages().send(player, "home.teleported");
                return;
            }
            player.sendActionBar(Component.text("\u23F3 " + (left / 1000 + 1) + "s"));
        }, 0L, 5L);
    }

    /** 取消传送。 */
    public void cancel(UUID uuid, boolean notify) {
        PendingTeleport teleport = pending.remove(uuid);
        if (teleport == null) {
            return;
        }
        if (teleport.task != null) {
            teleport.task.cancel();
        }
        if (notify) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                plugin.getMessages().send(player, "home.moved");
            }
        }
    }

    public void cancelAll() {
        for (UUID uuid : new ConcurrentHashMap<>(pending).keySet()) {
            cancel(uuid, false);
        }
    }

    /** 判断玩家是否因为移动而需要取消传送。 */
    public void checkMove(Player player, Location to) {
        PendingTeleport teleport = pending.get(player.getUniqueId());
        if (teleport == null || !plugin.getConfigManager().homeCancelOnMove()) {
            return;
        }
        Location origin = teleport.origin;
        if (origin == null || to == null) {
            return;
        }
        if (origin.getWorld() != to.getWorld()
                || origin.getBlockX() != to.getBlockX()
                || origin.getBlockY() != to.getBlockY()
                || origin.getBlockZ() != to.getBlockZ()) {
            cancel(player.getUniqueId(), true);
        }
    }
}
