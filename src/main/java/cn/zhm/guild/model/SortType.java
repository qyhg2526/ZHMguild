package cn.zhm.guild.model;

import java.util.Comparator;

/**
 * 公会排序方式。
 */
public enum SortType {

    LEVEL("等级", Comparator.comparingInt(Guild::getLevel).reversed()
            .thenComparing(Comparator.comparingInt(Guild::getActive).reversed())),
    ACTIVE("活跃", Comparator.comparingInt(Guild::getActive).reversed()),
    MONTH_ACTIVE("月度活跃", Comparator.comparingInt(Guild::getMonthActive).reversed()),
    FUNDS("资金", Comparator.comparingDouble(Guild::getFunds).reversed()),
    MEMBERS("人数", Comparator.comparingInt(Guild::getMemberCount).reversed()),
    CREATE_TIME("创建时间", Comparator.comparingLong(Guild::getCreateTime));

    private final String display;
    private final Comparator<Guild> comparator;

    SortType(String display, Comparator<Guild> comparator) {
        this.display = display;
        this.comparator = comparator;
    }

    public String display() {
        return display;
    }

    public Comparator<Guild> comparator() {
        return comparator;
    }

    public SortType next() {
        SortType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static SortType parse(String input, SortType fallback) {
        if (input == null) {
            return fallback;
        }
        for (SortType type : values()) {
            if (type.name().equalsIgnoreCase(input) || type.display.equals(input)) {
                return type;
            }
        }
        return switch (input.toLowerCase()) {
            case "prosperity_degree", "活跃度" -> ACTIVE;
            case "month_prosperity_degree", "月度活跃度" -> MONTH_ACTIVE;
            case "money", "资金" -> FUNDS;
            case "member_count", "人数" -> MEMBERS;
            case "create_time", "创建时间" -> CREATE_TIME;
            case "level", "等级" -> LEVEL;
            default -> fallback;
        };
    }
}
