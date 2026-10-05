package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 公会设置界面。
 */
public class SettingsMenu extends Gui {

    public SettingsMenu(ZHMguildPlugin plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        return titleOf("settings", "<dark_gray>公会设置</dark_gray> <gray>»</gray> <yellow>{guild}</yellow>",
                Placeholders.of().put("guild", guild == null ? "" : guild.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("settings", 27);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember self = plugin.getGuildManager().getMember(viewer.getUniqueId());
        if (guild == null || self == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }
        if (!self.getRole().atLeast(GuildRole.VICE)) {
            plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
            plugin.getGuiManager().openMain(viewer);
            return;
        }

        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .put("rename_cost", TimeUtil.money(plugin.getConfigManager().renameCost()))
                .putRaw("notice", guild.getNotice().isEmpty() ? "无" : Text.escape(guild.getNotice()))
                .putRaw("pvp_status", guild.isPvp() ? "<green>已开启</green>" : "<red>已关闭</red>");

        button("settings", "rename", GuiUtil.def(10, "NAME_TAG", "<yellow>修改公会名称</yellow>",
                        "<gray>当前名称: <yellow>{guild}</yellow></gray>",
                        "<gray>花费 <yellow>{rename_cost}</yellow> 金币</gray>",
                        "", "<yellow>» 点击修改</yellow>"),
                placeholders, event -> plugin.getChatInputManager().request(viewer, "settings.rename-input", 30, input -> {
                    if (!plugin.getGuildManager().isValidName(input, viewer)) {
                        return;
                    }
                    String message = plugin.getMessages().str("settings.rename-confirm", Placeholders.of()
                            .put("money", TimeUtil.money(plugin.getConfigManager().renameCost())));
                    plugin.getGuiManager().openConfirm(viewer, message, () -> {
                        plugin.getGuildActions().rename(viewer, guild, input);
                        plugin.getGuiManager().openSettings(viewer);
                    });
                }));

        button("settings", "notice", GuiUtil.def(11, "WRITABLE_BOOK", "<yellow>修改公会公告</yellow>",
                        "<gray>当前公告: <white>{notice}</white></gray>",
                        "", "<yellow>» 点击修改</yellow>"),
                placeholders, event -> plugin.getChatInputManager().request(viewer, "notice.input", 30, input -> {
                    plugin.getGuildActions().setNotice(viewer, guild, input);
                    plugin.getGuiManager().openSettings(viewer);
                }));

        button("settings", "icon", GuiUtil.def(12, "ITEM_FRAME", "<yellow>修改公会图标</yellow>",
                        "<gray>手持物品点击以设置为公会图标</gray>",
                        "", "<yellow>» 点击设置</yellow>"),
                placeholders, event -> {
                    plugin.getGuildActions().setIcon(viewer, guild);
                    refresh();
                });

        button("settings", "pvp", GuiUtil.def(13, "DIAMOND_SWORD", "<yellow>公会 PVP</yellow>",
                        "<gray>当前状态: {pvp_status}</gray>",
                        "<gray>开启后公会成员之间可以互相攻击</gray>",
                        "", "<yellow>» 点击切换</yellow>"),
                placeholders, event -> {
                    plugin.getGuildActions().togglePvp(viewer, guild);
                    refresh();
                });

        button("settings", "info", GuiUtil.def(15, "NETHER_STAR", "<aqua>公会信息</aqua>",
                        "<gray>返回公会主界面</gray>",
                        "", "<yellow>» 点击返回</yellow>"),
                placeholders, event -> plugin.getGuiManager().openMain(viewer));

        if (self.isLeader()) {
            button("settings", "dissolve", GuiUtil.def(16, "TNT", "<dark_red>解散公会</dark_red>",
                            "<red>解散后所有数据将被清空!</red>",
                            "<red>此操作不可逆!</red>",
                            "", "<yellow>» 点击解散</yellow>"),
                    placeholders, event -> plugin.getGuiManager().openConfirm(viewer,
                            plugin.getMessages().str("dissolve.confirm"), () -> {
                                plugin.getGuildActions().dissolve(viewer, guild);
                                viewer.closeInventory();
                            }));
        }

        addBackButton(() -> plugin.getGuiManager().openMain(viewer));
        fillEmpty();
    }
}
