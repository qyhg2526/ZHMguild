package cn.zhm.guild.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简易占位符容器, 使用 {key} 语法。
 */
public final class Placeholders {

    private final Map<String, String> values = new LinkedHashMap<>();

    private Placeholders() {
    }

    public static Placeholders of() {
        return new Placeholders();
    }

    public static Placeholders of(String key, Object value) {
        return new Placeholders().put(key, value);
    }

    public Placeholders put(String key, Object value) {
        values.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    public Placeholders putRaw(String key, String value) {
        values.put(key, value == null ? "" : value);
        return this;
    }

    public Placeholders putAll(Map<String, ?> map) {
        map.forEach(this::put);
        return this;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, String> asMap() {
        return values;
    }

    /** 把模板中的 {key} 替换为对应值。 */
    public String apply(String template) {
        if (template == null || template.isEmpty() || values.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /** 转义后的替换, 用于把玩家输入塞进 MiniMessage 模板。 */
    public Placeholders putEscaped(String key, String value, int maxLength) {
        values.put(key, Text.safe(value, maxLength));
        return this;
    }
}
