package cn.zhm.guild.config;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.util.ItemBuilder;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * gui.yml 读取器, 提供菜单标题、大小与按钮物品。
 */
public class GuiConfig {

    /**
     * gui.yml 结构版本。
     * 每次调整菜单布局(尤其是 size 与按钮槽位)时 +1,
     * 旧版本的 gui.yml 会被备份为 gui.yml.vN.bak 并重新生成, 避免旧布局导致按钮丢失。
     */
    public static final int CONFIG_VERSION = 2;

    private final ZHMguildPlugin plugin;
    private FileConfiguration cfg;

    public GuiConfig(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (file.exists()) {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
            int version = existing.getInt("config-version", 1);
            if (version < CONFIG_VERSION) {
                File backup = new File(plugin.getDataFolder(), "gui.yml.v" + version + ".bak");
                try {
                    java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.saveResource("gui.yml", true);
                    plugin.getLogger().info("gui.yml 菜单布局已升级到 v" + CONFIG_VERSION
                            + ", 旧文件已备份为 " + backup.getName());
                } catch (Exception exception) {
                    plugin.getLogger().warning("更新 gui.yml 失败, 将继续使用旧配置: " + exception.getMessage());
                }
            }
        } else {
            plugin.saveResource("gui.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = plugin.getResource("gui.yml");
        if (defaults != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public FileConfiguration raw() {
        return cfg;
    }

    /** 菜单标题原始文本。 */
    public String title(String menu, String fallback) {
        return cfg.getString(menu + ".title", fallback);
    }

    public int size(String menu, int fallback) {
        int size = cfg.getInt(menu + ".size", fallback);
        if (size < 9 || size > 54) {
            size = fallback;
        }
        return (size / 9) * 9;
    }

    /** 取 menu 下 items.<key> 定义的按钮。 */
    public Optional<GuiItemDef> item(String menu, String key) {
        ConfigurationSection section = cfg.getConfigurationSection(menu + ".items." + key);
        return section == null ? Optional.empty() : Optional.of(GuiItemDef.from(section, -1));
    }

    /** 取 menu 下 items.<key> 定义的按钮, 不存在时使用默认值。 */
    public GuiItemDef itemOr(String menu, String key, GuiItemDef fallback) {
        return item(menu, key).orElse(fallback);
    }

    /** 取某个模板定义, 例如 guild-list.guild-item。 */
    public GuiItemDef template(String path, GuiItemDef fallback) {
        ConfigurationSection section = cfg.getConfigurationSection(path);
        return section == null ? fallback : GuiItemDef.from(section, -1);
    }

    public GuiItemDef common(String key) {
        ConfigurationSection section = cfg.getConfigurationSection("common." + key);
        return section == null ? null : GuiItemDef.from(section, -1);
    }

    public int commonSlot(String key, int fallback) {
        return cfg.getInt("common." + key + ".slot", fallback);
    }

    public String string(String path, String fallback) {
        return cfg.getString(path, fallback);
    }

    public List<String> stringList(String path, List<String> fallback) {
        List<String> list = cfg.getStringList(path);
        return list.isEmpty() ? fallback : list;
    }

    /** 全部菜单键。 */
    public static final List<String> MENUS = List.of(
            "no-guild", "main", "guild-war", "war-arena", "war-reward", "guild-list", "members",
            "member-manage", "applications", "settings", "top", "confirm");

    /** 模板与通用按钮路径。 */
    private static final List<String> TEMPLATES = List.of(
            "common.filler", "common.back", "common.close", "common.previous", "common.next",
            "guild-list.guild-item", "members.member-item", "applications.application-item",
            "top.entry-item", "guild-war.ally-item", "guild-war.enemy-item", "guild-war.member-item",
            "guild-war.empty-item", "war-arena.arena-item");

    /** 自检结果。 */
    public record ValidationResult(int checked, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    /**
     * 自检 GUI 配置: 尝试构建全部按钮物品, 返回校验数量与错误列表。
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (String menu : MENUS) {
            int size = size(menu, 54);
            ConfigurationSection items = cfg.getConfigurationSection(menu + ".items");
            if (items == null) {
                continue;
            }
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String path = menu + ".items." + key;
                GuiItemDef def = GuiItemDef.from(section, -1);
                checked++;
                if (def.slot() < 0 || def.slot() >= size) {
                    errors.add(path + ": 槽位 " + def.slot() + " 超出菜单大小 " + size);
                }
                buildCheck(errors, path, def);
            }
        }
        for (String path : TEMPLATES) {
            ConfigurationSection section = cfg.getConfigurationSection(path);
            checked++;
            if (section == null) {
                errors.add(path + ": 缺少配置");
                continue;
            }
            buildCheck(errors, path, GuiItemDef.from(section, -1));
        }
        return new ValidationResult(checked, errors);
    }

    private void buildCheck(List<String> errors, String path, GuiItemDef def) {
        try {
            if (Material.matchMaterial(def.material() == null ? "" : def.material().toUpperCase()) == null) {
                errors.add(path + ": 无效材质 " + def.material());
                return;
            }
            def.build();
        } catch (Throwable throwable) {
            errors.add(path + " -> " + throwable);
        }
    }

    /**
     * GUI 物品定义。
     */
    public static final class GuiItemDef {

        private final int slot;
        private final String material;
        private final String name;
        private final List<String> lore;
        private final boolean glow;
        private final int amount;
        private final Integer modelData;

        public GuiItemDef(int slot, String material, String name, List<String> lore,
                          boolean glow, int amount, Integer modelData) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.glow = glow;
            this.amount = amount;
            this.modelData = modelData;
        }

        public static GuiItemDef from(ConfigurationSection section, int fallbackSlot) {
            List<String> lore = new ArrayList<>(section.getStringList("lore"));
            return new GuiItemDef(
                    section.getInt("slot", fallbackSlot),
                    section.getString("material", "STONE"),
                    section.getString("name", ""),
                    lore,
                    section.getBoolean("glow", false),
                    Math.max(1, Math.min(64, section.getInt("amount", 1))),
                    section.isInt("custom-model-data") ? section.getInt("custom-model-data") : null
            );
        }

        public GuiItemDef withSlot(int newSlot) {
            return new GuiItemDef(newSlot, material, name, lore, glow, amount, modelData);
        }

        public int slot() {
            return slot;
        }

        public String material() {
            return material;
        }

        public String name() {
            return name;
        }

        public List<String> lore() {
            return lore;
        }

        public ItemStack build() {
            return build(Placeholders.of());
        }

        public ItemStack build(Placeholders placeholders) {
            Material mat = Material.matchMaterial(material == null ? "STONE" : material.toUpperCase());
            if (mat == null) {
                mat = Material.STONE;
            }
            ItemBuilder builder = ItemBuilder.of(mat, amount);
            if (name != null && !name.isEmpty()) {
                builder.name(Text.mm(placeholders.apply(name)));
            }
            if (lore != null && !lore.isEmpty()) {
                builder.lore(Text.mmList(lore, placeholders));
            }
            if (glow) {
                builder.glow(true);
            }
            if (modelData != null) {
                builder.modelData(modelData);
            }
            return builder.hideAll().build();
        }

        /** 使用默认值构建一个物品。 */
        public static ItemStack of(String material, String name, List<String> lore, Placeholders placeholders) {
            Material mat = Material.matchMaterial(material == null ? "STONE" : material.toUpperCase());
            ItemBuilder builder = ItemBuilder.of(mat == null ? Material.STONE : mat);
            if (name != null && !name.isEmpty()) {
                builder.name(Text.mm(placeholders.apply(name)));
            }
            if (lore != null && !lore.isEmpty()) {
                builder.lore(Text.mmList(lore, placeholders));
            }
            return builder.hideAll().build();
        }
    }
}
