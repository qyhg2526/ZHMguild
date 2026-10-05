package cn.zhm.guild.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 时间与数字格式化工具。
 */
public final class TimeUtil {

    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd");

    private TimeUtil() {
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) {
            return "无";
        }
        return DATE_TIME.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        if (millis <= 0) {
            return "无";
        }
        return DATE.format(new Date(millis));
    }

    /** 把剩余毫秒格式化为 "1天2小时3分4秒"。 */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0秒";
        }
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        return sb.toString();
    }

    /** 保留两位小数, 去掉多余的 .0。 */
    public static String money(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    /** 判断两个时间戳是否在同一天(本地时区)。 */
    public static boolean sameDay(long first, long second) {
        return formatDate(first).equals(formatDate(second));
    }
}
