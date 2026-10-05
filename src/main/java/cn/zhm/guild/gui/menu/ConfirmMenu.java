package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.util.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 通用确认界面。
 */
public class ConfirmMenu extends Gui {

    private final String message;
    private final Runnable onConfirm;

    public ConfirmMenu(ZHMguildPlugin plugin, Player viewer, String message, Runnable onConfirm) {
        super(plugin, viewer);
        this.message = message;
        this.onConfirm = onConfirm;
    }

    @Override
    protected Component title() {
        return titleOf("confirm", "<dark_gray>确认操作</dark_gray>", Placeholders.of());
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("confirm", 27);
    }

    @Override
    protected void build() {
        Placeholders placeholders = Placeholders.of().putRaw("message", message);
        button("confirm", "confirm", GuiUtil.def(11, "LIME_CONCRETE", "<green>确认</green>",
                "<gray>{message}</gray>", "", "<yellow>» 点击确认</yellow>"), placeholders, event -> {
            viewer.closeInventory();
            onConfirm.run();
        });
        button("confirm", "cancel", GuiUtil.def(15, "RED_CONCRETE", "<red>取消</red>",
                "<gray>放弃该操作</gray>", "", "<yellow>» 点击取消</yellow>"), placeholders,
                event -> viewer.closeInventory());
        fillEmpty();
    }
}
