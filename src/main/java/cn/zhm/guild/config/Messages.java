package cn.zhm.guild.config;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * messages.yml 读取器。
 */
public class Messages {

    private final ZHMguildPlugin plugin;
    private FileConfiguration cfg;
    private String prefix = "";

    public Messages(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
        prefix = cfg.getString("prefix", "");
    }

    public void reload() {
        load();
    }

    /** 原始文本(未替换、未解析)。 */
    public String raw(String path) {
        if (cfg == null) {
            return "";
        }
        String value = cfg.getString(path);
        if (value == null) {
            return "<red>[缺失语言: " + path + "]</red>";
        }
        return value;
    }

    /** 是否配置了该语言项。 */
    public boolean has(String path) {
        return cfg != null && cfg.isString(path);
    }

    /** 替换占位符后的文本。 */
    public String str(String path, Placeholders placeholders) {
        String value = raw(path);
        if (placeholders != null) {
            value = placeholders.apply(value);
        }
        return value;
    }

    public String str(String path) {
        return str(path, null);
    }

    /** 解析为 Component, 不含前缀。 */
    public Component component(String path, Placeholders placeholders) {
        return Text.mm(str(path, placeholders));
    }

    /** 解析为 Component, 含前缀。 */
    public Component prefixed(String path, Placeholders placeholders) {
        return Text.mm(prefix + str(path, placeholders));
    }

    public String prefix() {
        return prefix;
    }

    /** 发送一条消息给玩家(带前缀 + PAPI 支持)。 */
    public void send(CommandSender sender, String path, Placeholders placeholders) {
        if (sender == null) {
            return;
        }
        String raw = prefix + str(path, placeholders);
        sender.sendMessage(parse(raw, sender));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    /** 发送不含前缀的消息。 */
    public void sendRaw(CommandSender sender, String path, Placeholders placeholders) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(parse(str(path, placeholders), sender));
    }

    public void send(CommandSender sender, Component component) {
        if (sender != null && component != null) {
            sender.sendMessage(component);
        }
    }

    /** 解析文本: 先处理 PAPI 变量, 再把传统颜色代码转成 MiniMessage, 最后反序列化。 */
    public Component parse(String raw, CommandSender sender) {
        String text = raw == null ? "" : raw;
        if (plugin.getConfigManager().placeholderApi() && plugin.getPlaceholderHook() != null && sender != null) {
            text = plugin.getPlaceholderHook().apply(sender, text);
        }
        return Text.mm(Text.legacyToMiniMessage(text));
    }

    public Component parse(String raw, Player player) {
        return parse(raw, (CommandSender) player);
    }
}
