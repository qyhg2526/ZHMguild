package cn.zhm.guild.hook;

import cn.zhm.guild.ZHMguildPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * PlayerPoints 点券挂钩 (反射实现, 未安装时自动降级)。
 */
public class PointsHook {

    private final ZHMguildPlugin plugin;
    private Object api;
    private Method lookMethod;
    private Method giveMethod;
    private Method takeMethod;

    public PointsHook(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        Plugin playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (playerPoints == null) {
            return false;
        }
        try {
            Method getApi = playerPoints.getClass().getMethod("getAPI");
            api = getApi.invoke(playerPoints);
            if (api == null) {
                return false;
            }
            lookMethod = api.getClass().getMethod("look", UUID.class);
            giveMethod = api.getClass().getMethod("give", UUID.class, int.class);
            takeMethod = api.getClass().getMethod("take", UUID.class, int.class);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("PlayerPoints 挂钩失败: " + throwable.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return api != null && lookMethod != null;
    }

    public int look(UUID uuid) {
        if (!isEnabled() || uuid == null) {
            return 0;
        }
        try {
            Object result = lookMethod.invoke(api, uuid);
            return result instanceof Number number ? number.intValue() : 0;
        } catch (Throwable throwable) {
            return 0;
        }
    }

    public boolean give(UUID uuid, int amount) {
        if (!isEnabled() || uuid == null || amount <= 0) {
            return false;
        }
        try {
            Object result = giveMethod.invoke(api, uuid, amount);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public boolean take(UUID uuid, int amount) {
        if (!isEnabled() || uuid == null || amount <= 0) {
            return true;
        }
        try {
            Object result = takeMethod.invoke(api, uuid, amount);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
