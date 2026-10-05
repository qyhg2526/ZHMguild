package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Arena;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.model.GuildWar;
import cn.zhm.guild.util.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.StringJoiner;

/**
 * 匹配公会战界面。
 */
public class GuildWarMenu extends Gui {

    public GuildWarMenu(ZHMguildPlugin plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        return titleOf("guild-war", "<dark_gray>匹配公会战</dark_gray> <gray>»</gray> <yellow>{guild}</yellow>",
                Placeholders.of().put("guild", guild == null ? "" : guild.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("guild-war", 27);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(viewer.getUniqueId());
        if (guild == null || member == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }

        StringJoiner arenaNames = new StringJoiner("<dark_gray>, </dark_gray>");
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            arenaNames.add((arena.isAvailable() ? "<green>" : arena.isInUse() ? "<red>" : "<gray>")
                    + arena.getName());
        }
        if (arenaNames.length() == 0) {
            arenaNames.add("<red>未设置</red>");
        }

        int remaining = plugin.getGuildWarManager().remainingToday(guild);
        GuildWar war = plugin.getGuildWarManager().getWarOfGuild(guild);

        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .putRaw("war_status", plugin.getGuildWarManager().statusText(guild))
                .put("queue_size", plugin.getGuildWarManager().queueSize())
                .putRaw("daily_left", remaining < 0 ? "<green>无限制</green>" : (remaining > 0 ? "<yellow>" + remaining + "</yellow>" : "<red>已用完</red>"))
                .put("daily_used", plugin.getGuildWarManager().usedToday(guild))
                .put("daily_limit", plugin.getConfigManager().guildWarDailyLimit())
                .put("online_members", plugin.getGuildWarManager().onlineMembers(guild).size())
                .put("max_per_side", plugin.getConfigManager().guildWarMaxPerSide())
                .put("prepare_seconds", plugin.getConfigManager().guildWarPrepareSeconds())
                .put("battle_seconds", plugin.getConfigManager().guildWarBattleSeconds())
                .putRaw("arenas", arenaNames.toString())
                .putRaw("enemy", war == null ? "<gray>无</gray>" : war.getOpponent(guild).getName())
                .putRaw("state", war == null ? "<gray>无进行中的战斗</gray>" : war.getState().name());

        button("guild-war", "info", GuiUtil.def(4, "DIAMOND_SWORD", "<red>匹配公会战</red>",
                        "<gray>当前状态: {war_status}</gray>",
                        "<gray>队列中公会: <yellow>{queue_size}</yellow></gray>",
                        "<gray>今日剩余次数: {daily_left} <dark_gray>({daily_used}/{daily_limit})</dark_gray></gray>",
                        "<gray>可参战成员: <yellow>{online_members}</yellow><dark_gray>/</dark_gray><yellow>{max_per_side}</yellow></gray>",
                        "",
                        "<gray>场地: {arenas}</gray>"),
                placeholders, null);

        button("guild-war", "match", GuiUtil.def(11, "LIME_CONCRETE", "<green>开始匹配</green>",
                        "<gray>加入匹配队列</gray>",
                        "<gray>队列满 2 个公会后自动开战</gray>",
                        "<gray>准备阶段 <yellow>{prepare_seconds}</yellow> 秒, 战斗 <yellow>{battle_seconds}</yellow> 秒</gray>",
                        "",
                        "<yellow>» 点击开始匹配</yellow>"),
                placeholders, event -> {
                    if (member.getRole().atLeast(GuildRole.VICE)) {
                        plugin.getGuildWarManager().enqueue(guild, viewer);
                    } else {
                        plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                                .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
                    }
                    refresh();
                });

        button("guild-war", "cancel", GuiUtil.def(13, "RED_CONCRETE", "<red>取消匹配</red>",
                        "<gray>把公会移出匹配队列</gray>",
                        "",
                        "<yellow>» 点击取消匹配</yellow>"),
                placeholders, event -> {
                    if (member.getRole().atLeast(GuildRole.VICE)) {
                        plugin.getGuildWarManager().dequeue(guild, viewer);
                    } else {
                        plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                                .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
                    }
                    refresh();
                });

        button("guild-war", "leave", GuiUtil.def(15, "IRON_DOOR", "<yellow>离开战斗</yellow>",
                        "<gray>准备阶段可无损失退出</gray>",
                        "<gray>战斗阶段退出视为被淘汰</gray>",
                        "",
                        "<yellow>» 点击离开</yellow>"),
                placeholders, event -> {
                    plugin.getGuildWarManager().leave(viewer);
                    refresh();
                });

        addBackButton(() -> plugin.getGuiManager().openMain(viewer));
        fillEmpty();
    }
}
