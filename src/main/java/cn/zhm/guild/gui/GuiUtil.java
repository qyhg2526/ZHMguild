package cn.zhm.guild.gui;

import cn.zhm.guild.config.GuiConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI 工具。
 */
public final class GuiUtil {

    /** 54 格菜单的可用内容槽位。 */
    public static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private GuiUtil() {
    }

    public static int pages(int total, int perPage) {
        if (perPage <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(total / (double) perPage));
    }

    public static GuiConfig.GuiItemDef def(int slot, String material, String name, String... lore) {
        return new GuiConfig.GuiItemDef(slot, material, name, new ArrayList<>(Arrays.asList(lore)), false, 1, null);
    }

    public static GuiConfig.GuiItemDef def(String material, String name, String... lore) {
        return def(-1, material, name, lore);
    }

    public static List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    /** 把坐标描述成 "world x,y,z" 形式。 */
    public static String describe(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "未设置";
        }
        return location.getWorld().getName() + " "
                + (int) location.getX() + "," + (int) location.getY() + "," + (int) location.getZ();
    }
}
