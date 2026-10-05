package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 我的公会主界面。
 */
public class MainMenu extends Gui {

    public MainMenu(ZHMguildPlugin plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        return titleOf("main", "<dark_gray>我的公会</dark_gray> <gray>»</gray> <yellow>{guild}</yellow>",
                Placeholders.of().put("guild", guild == null ? "" : guild.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("main", 54);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(viewer.getUniqueId());
        if (guild == null || member == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }

        List<Double> presets = plugin.getConfigManager().contributePresets();
        Location home = guild.getSafeHome();
        String homeText = home == null ? "未设置"
                : home.getWorld().getName() + " " + (int) home.getX() + "," + (int) home.getY() + "," + (int) home.getZ();

        int online = plugin.getGuildWarManager().onlineCount(guild);
        int[] record = plugin.getGuildWarManager().getRecord(guild);
        int warRemaining = plugin.getGuildWarManager().remainingToday(guild);
        String warRecord = "<green>" + record[0] + " 胜</green> <red>" + record[1] + " 负</red> <gray>" + record[2] + " 平</gray>";

        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .put("leader", guild.getLeaderName())
                .put("level", guild.getLevel())
                .put("members", guild.getMemberCount())
                .put("max_members", plugin.getGuildManager().maxMembers(guild))
                .put("online", online)
                .put("active", guild.getActive())
                .put("month_active", guild.getMonthActive())
                .put("funds", TimeUtil.money(guild.getFunds()))
                .put("ore", guild.getOre())
                .put("create_time", TimeUtil.formatDate(guild.getCreateTime()))
                .putRaw("notice", guild.getNotice().isEmpty() ? "无" : Text.escape(guild.getNotice()))
                .put("applications", guild.getApplicationCount())
                .put("up_money", TimeUtil.money(plugin.getGuildManager().upMoney(guild)))
                .put("up_active", plugin.getGuildManager().upActive(guild))
                .put("home", homeText)
                .put("streak", member.getSignStreak())
                .put("reward_money", TimeUtil.money(plugin.getConfigManager().signInMoney()))
                .put("reward_contribution", TimeUtil.money(plugin.getConfigManager().signInContribution()))
                .put("contribution", TimeUtil.money(member.getContribution()))
                .put("contribute_1", presets.size() > 0 ? TimeUtil.money(presets.get(0)) : "1000")
                .put("contribute_2", presets.size() > 1 ? TimeUtil.money(presets.get(1)) : "10000")
                .putRaw("war_status", plugin.getGuildWarManager().statusText(guild))
                .put("war_online", online)
                .put("war_participants", plugin.getGuildWarManager().participantCount(guild))
                .putRaw("war_record", warRecord)
                .putRaw("war_daily_left", warRemaining < 0 ? "<green>无限制</green>"
                        : (warRemaining > 0 ? "<yellow>" + warRemaining + "</yellow>" : "<red>已用完</red>"));

        button("main", "info", GuiUtil.def(4, "NETHER_STAR", "<yellow>{guild}</yellow>",
                "<gray>会长: <yellow>{leader}</yellow></gray>",
                "<gray>等级: <yellow>{level}</yellow></gray>",
                "<gray>成员: <yellow>{members}</yellow>/<yellow>{max_members}</yellow></gray>",
                "<gray>在线人口: <yellow>{online}</yellow> 人</gray>",
                "<gray>活跃: <yellow>{active}</yellow></gray>",
                "<gray>资金: <yellow>{funds}</yellow></gray>"), placeholders, null);

        button("main", "members", GuiUtil.def(19, "PLAYER_HEAD", "<aqua>成员列表</aqua>",
                        "<gray>查看公会中的 <yellow>{members}</yellow> 名成员</gray>",
                        "", "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openMembers(viewer, 1));

        button("main", "applications", GuiUtil.def(20, "PAPER", "<green>入会申请</green>",
                        "<gray>待处理申请: <yellow>{applications}</yellow></gray>",
                        "", "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openApplications(viewer, 1));

        button("main", "upgrade", GuiUtil.def(21, "EXPERIENCE_BOTTLE", "<light_purple>升级公会</light_purple>",
                        "<gray>当前等级: <yellow>{level}</yellow></gray>",
                        "<gray>需要资金: <yellow>{up_money}</yellow></gray>",
                        "<gray>需要活跃: <yellow>{up_active}</yellow></gray>",
                        "", "<yellow>» 点击升级</yellow>"),
                placeholders, event -> {
                    plugin.getGuildManager().upgrade(guild, viewer);
                    refresh();
                });

        button("main", "home", GuiUtil.def(22, "RED_BED", "<gold>公会主城</gold>",
                        "<gray>位置: <yellow>{home}</yellow></gray>",
                        "", "<yellow>» 左键传送</yellow>",
                        "<yellow>» 潜行左键设置主城</yellow>"),
                placeholders, event -> {
                    if (event.isShiftClick()) {
                        plugin.getGuildActions().setHome(viewer, guild, member);
                    } else {
                        plugin.getGuildActions().teleportHome(viewer, guild);
                        viewer.closeInventory();
                    }
                });

        button("main", "signin", GuiUtil.def(23, "CLOCK", "<yellow>每日签到</yellow>",
                        "<gray>连续签到: <yellow>{streak}</yellow> 天</gray>",
                        "<gray>今日奖励: <yellow>{reward_money}</yellow> 金币 + <yellow>{reward_contribution}</yellow> 贡献</gray>",
                        "", "<yellow>» 点击签到</yellow>"),
                placeholders, event -> {
                    plugin.getGuildManager().signIn(viewer, guild, member);
                    refresh();
                });

        button("main", "contribute", GuiUtil.def(24, "GOLD_INGOT", "<gold>贡献公会</gold>",
                        "<gray>你的贡献值: <yellow>{contribution}</yellow></gray>",
                        "",
                        "<yellow>» 左键贡献 {contribute_1} 金币</yellow>",
                        "<yellow>» 右键贡献 {contribute_2} 金币</yellow>"),
                placeholders, event -> {
                    double amount;
                    if (event.isRightClick()) {
                        amount = presets.size() > 1 ? presets.get(1) : 10000.0D;
                    } else {
                        amount = presets.isEmpty() ? 1000.0D : presets.get(0);
                    }
                    plugin.getGuildActions().contribute(viewer, guild, member, amount);
                    refresh();
                });

        button("main", "list", GuiUtil.def(25, "BOOK", "<aqua>公会列表</aqua>",
                        "<gray>查看服务器中的所有公会</gray>",
                        "", "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openGuildList(viewer, SortType.LEVEL, 1));

        button("main", "settings", GuiUtil.def(30, "COMPARATOR", "<yellow>公会设置</yellow>",
                        "<gray>修改公告 / 名称 / 图标 / PVP</gray>",
                        "", "<yellow>» 点击进入</yellow>"),
                placeholders, event -> {
                    if (member.getRole().atLeast(GuildRole.VICE)) {
                        plugin.getGuiManager().openSettings(viewer);
                    } else {
                        plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                                .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
                    }
                });

        button("main", "war", GuiUtil.def(31, "DIAMOND_SWORD", "<red>匹配公会战</red>",
                        "<gray>当前状态: {war_status}</gray>",
                        "<gray>可参战人口: <yellow>{war_online}</yellow> 人</gray>",
                        "<gray>战绩: {war_record}</gray>",
                        "<gray>今日剩余次数: {war_daily_left}</gray>",
                        "",
                        "<yellow>» 点击进入公会战</yellow>"),
                placeholders, event -> plugin.getGuiManager().openGuildWar(viewer));

        button("main", "top", GuiUtil.def(32, "GOLDEN_HELMET", "<gold>公会排行</gold>",
                        "<gray>查看服务器最强公会</gray>",
                        "", "<yellow>» 点击查看</yellow>"),
                placeholders, event -> plugin.getGuiManager().openTop(viewer, SortType.LEVEL, 1));

        if (!member.isLeader()) {
            button("main", "leave", GuiUtil.def(40, "IRON_DOOR", "<red>退出公会</red>",
                            "<gray>离开当前公会</gray>",
                            "", "<yellow>» 点击退出</yellow>"),
                    placeholders, event -> plugin.getGuiManager().openConfirm(viewer,
                            plugin.getMessages().str("leave.confirm", Placeholders.of().put("guild", guild.getName())),
                            () -> plugin.getGuildActions().leave(viewer)));
        }

        addCloseButton();
        fillEmpty();
    }
}
