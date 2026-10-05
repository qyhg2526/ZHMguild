package cn.zhm.guild.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MiniMessage 文本工具。
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** 反序列化 MiniMessage 文本。 */
    public static Component mm(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return MM.deserialize(raw);
    }

    /** 反序列化并替换占位符。 */
    public static Component mm(String raw, Placeholders placeholders) {
        return mm(placeholders.apply(raw));
    }

    /** 取纯文本。 */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** 去掉全部格式, 得到纯文本。 */
    public static String strip(String raw) {
        return plain(mm(raw));
    }

    /** 转义用户输入, 防止其被当作 MiniMessage 标签解析。 */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("<", "\\<");
    }

    /** 转义并裁剪。 */
    public static String safe(String input, int maxLength) {
        String s = input == null ? "" : input.trim();
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength);
        }
        return escape(s);
    }

    /** 序列化为传统 § 格式, 供 PlaceholderAPI 等使用。 */
    public static String legacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(component == null ? Component.empty() : component);
    }

    /** MiniMessage 文本 -> 传统 § 格式。 */
    public static String legacy(String miniMessage) {
        return legacy(mm(miniMessage));
    }

    private static final String LEGACY_CHARS = "0123456789abcdefklmnor";
    private static final String[] LEGACY_TAGS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            "obfuscated", "bold", "strikethrough", "underlined", "italic", "reset"
    };

    /**
     * 把传统颜色代码(&a / §a)转换成 MiniMessage 标签。
     * 主要用于兼容 PlaceholderAPI 返回的传统格式文本。
     */
    public static String legacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (input.indexOf('&') < 0 && input.indexOf('\u00A7') < 0) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00A7') && i + 1 < input.length()) {
                int index = LEGACY_CHARS.indexOf(Character.toLowerCase(input.charAt(i + 1)));
                if (index >= 0) {
                    String tag = LEGACY_TAGS[index];
                    sb.append("reset".equals(tag) ? "<reset>" : "<" + tag + ">");
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** 批量反序列化。 */
    public static List<Component> mmList(List<String> raw) {
        return raw.stream().map(Text::mm).collect(Collectors.toList());
    }

    /** 批量反序列化 + 占位符。 */
    public static List<Component> mmList(List<String> raw, Placeholders placeholders) {
        return raw.stream().map(s -> mm(placeholders.apply(s))).collect(Collectors.toList());
    }
}
