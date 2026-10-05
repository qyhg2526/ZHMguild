package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公会战场地管理。场地保存在 arena.yml。
 */
public class ArenaManager {

    /** 场地指针: 1 = 红队出生点, 2 = 蓝队出生点, 3 = 观看点。 */
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int SPECTATE = 3;

    private final ZHMguildPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private File file;

    public ArenaManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------
    // 读写
    // ---------------------------------------------------------

    public void load() {
        arenas.clear();
        file = new File(plugin.getDataFolder(), "arena.yml");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("已创建 arena.yml");
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("创建 arena.yml 失败: " + exception.getMessage());
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            Arena arena = new Arena(name);
            arena.setRedSpawn(readLocation(section.getConfigurationSection("red")));
            arena.setBlueSpawn(readLocation(section.getConfigurationSection("blue")));
            arena.setSpectateSpawn(readLocation(section.getConfigurationSection("spectate")));
            arenas.put(name.toLowerCase(), arena);
        }
        plugin.getLogger().info("已载入 " + arenas.size() + " 个公会战场地。");
    }

    public void save() {
        if (file == null) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of(
                "ZHMguild 公会战场地配置",
                "由 /zg setLocation mate <场地名> <1|2|3> 自动生成",
                "1 = 红队出生点, 2 = 蓝队出生点, 3 = 观看点(可选)"));
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.getName() + ".";
            writeLocation(config, base + "red", arena.getRedSpawn());
            writeLocation(config, base + "blue", arena.getBlueSpawn());
            writeLocation(config, base + "spectate", arena.getSpectateSpawn());
        }
        try {
            config.save(file);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 arena.yml 失败: " + exception.getMessage());
        }
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("场地世界不存在: " + worldName);
            return null;
        }
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    private void writeLocation(YamlConfiguration config, String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", (double) location.getYaw());
        config.set(path + ".pitch", (double) location.getPitch());
    }

    // ---------------------------------------------------------
    // 查询
    // ---------------------------------------------------------

    public Arena get(String name) {
        return name == null ? null : arenas.get(name.toLowerCase());
    }

    public Arena getOrCreate(String name) {
        Arena arena = get(name);
        if (arena == null) {
            arena = new Arena(name);
            arenas.put(name.toLowerCase(), arena);
        }
        return arena;
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public List<String> names() {
        return new ArrayList<>(arenas.values().stream().map(Arena::getName).toList());
    }

    public int size() {
        return arenas.size();
    }

    public boolean remove(String name) {
        Arena removed = arenas.remove(name == null ? "" : name.toLowerCase());
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    /** 找一个空闲且就绪的场地。 */
    public Arena findAvailable() {
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable()) {
                return arena;
            }
        }
        return null;
    }

    public boolean hasAvailableArena() {
        return findAvailable() != null;
    }

    // ---------------------------------------------------------
    // 设置
    // ---------------------------------------------------------

    /**
     * 设置场地出生点。
     *
     * @param index 1 = 红队, 2 = 蓝队, 3 = 观看点
     */
    public boolean setSpawn(String arenaName, int index, Location location) {
        if (arenaName == null || arenaName.isEmpty() || location == null) {
            return false;
        }
        Arena arena = getOrCreate(arenaName);
        switch (index) {
            case RED -> arena.setRedSpawn(location);
            case BLUE -> arena.setBlueSpawn(location);
            case SPECTATE -> arena.setSpectateSpawn(location);
            default -> {
                return false;
            }
        }
        save();
        return true;
    }

    /** 全服公会战结束后释放所有占用(用于插件启用/重载)。 */
    public void releaseAll() {
        for (Arena arena : arenas.values()) {
            arena.release();
        }
    }
}
