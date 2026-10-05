package cn.zhm.guild.model;

/**
 * 申请类型。
 */
public enum ApplicationType {

    /** 玩家主动申请加入。 */
    APPLY("申请"),
    /** 公会主动邀请。 */
    INVITE("邀请");

    private final String display;

    ApplicationType(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public static ApplicationType parse(String input) {
        if (input == null) {
            return APPLY;
        }
        return "INVITE".equalsIgnoreCase(input) ? INVITE : APPLY;
    }
}
