package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 未加入公会时的主界面。
 */
public class NoGuildMenu extends Gui {

    public NoGuildMenu(ZHMguildPlugin plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        return titleOf("no-guild", "<dark_gray>公会</dark_gray> <gray>»</gray> <yellow>未加入公会</yellow>", Placeholders.of());
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("no-guild", 27);
    }

    @Override
    protected void build() {
        double balance = plugin.getEconomyHook().isEnabled()
                ? plugin.getEconomyHook().getBalance(viewer)
                : plugin.getPointsHook().isEnabled() ? plugin.getPointsHook().look(viewer.getUniqueId()) : 0;
        Placeholders placeholders = Placeholders.of()
                .put("create_money", TimeUtil.money(plugin.getConfigManager().createMoney()))
                .put("create_points", plugin.getConfigManager().createPoints())
                .put("balance", TimeUtil.money(balance))
                .put("total", plugin.getGuildManager().getGuildCount());

        button("no-guild", "create", GuiUtil.def(11, "EMERALD_BLOCK", "<green>创建公会</green>",
                        "<gray>花费 <yellow>{create_money}</yellow> 金币创建属于你的公会</gray>",
                        "<gray>当前余额: <yellow>{balance}</yellow></gray>",
                        "",
                        "<yellow>» 点击创建</yellow>"),
                placeholders, event -> plugin.getGuiManager().beginCreate(viewer));

        button("no-guild", "list", GuiUtil.def(13, "BOOK", "<aqua>公会列表</aqua>",
                        "<gray>查看服务器中的所有公会</gray>",
                        "<gray>当前共有 <yellow>{total}</yellow> 个公会</gray>",
                        "",
                        "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openGuildList(viewer, SortType.LEVEL, 1));

        button("no-guild", "top", GuiUtil.def(15, "GOLDEN_HELMET", "<gold>公会排行</gold>",
                        "<gray>查看最强大的公会</gray>",
                        "",
                        "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openTop(viewer, SortType.LEVEL, 1));

        addCloseButton();
        fillEmpty();
    }
}
