package cn.zhm.guild.model;

/**
 * 公会职位。weight 越大职位越高。
 */
public enum GuildRole {

    MEMBER(1, "成员"),
    ELDER(2, "长老"),
    VICE(3, "副会长"),
    LEADER(4, "会长");

    private final int weight;
    private final String defaultDisplay;

    GuildRole(int weight, String defaultDisplay) {
        this.weight = weight;
        this.defaultDisplay = defaultDisplay;
    }

    public int weight() {
        return weight;
    }

    public String defaultDisplay() {
        return defaultDisplay;
    }

    /** 当前职位是否不低于 other。 */
    public boolean atLeast(GuildRole other) {
        return other != null && this.weight >= other.weight;
    }

    /** 职位是否高于 other。 */
    public boolean above(GuildRole other) {
        return other != null && this.weight > other.weight;
    }

    /** 提升一级, 已是会长则返回 null。 */
    public GuildRole promote() {
        return switch (this) {
            case MEMBER -> ELDER;
            case ELDER -> VICE;
            case VICE -> LEADER;
            case LEADER -> null;
        };
    }

    /** 降低一级, 已是成员则返回 null。 */
    public GuildRole demote() {
        return switch (this) {
            case LEADER -> VICE;
            case VICE -> ELDER;
            case ELDER -> MEMBER;
            case MEMBER -> null;
        };
    }

    public static GuildRole parse(String input, GuildRole fallback) {
        if (input == null) {
            return fallback;
        }
        for (GuildRole role : values()) {
            if (role.name().equalsIgnoreCase(input) || role.defaultDisplay.equals(input)) {
                return role;
            }
        }
        return switch (input.toLowerCase()) {
            case "owner", "master", "会长", "z" -> LEADER;
            case "admin", "vice", "副会长", "f" -> VICE;
            case "elder", "长老", "e" -> ELDER;
            case "member", "成员", "m" -> MEMBER;
            default -> fallback;
        };
    }
}
