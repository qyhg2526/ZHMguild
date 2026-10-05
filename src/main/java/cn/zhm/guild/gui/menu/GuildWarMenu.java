package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.config.GuiConfig;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Arena;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.model.GuildWar;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * 匹配公会战主界面。
 *
 * <p>布局: 状态总览 / 我方阵容 / 信息按钮 / 敌方阵容 / 操作按钮。</p>
 */
public class GuildWarMenu extends Gui {

    private static final int[] ALLY_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] ENEMY_SLOTS = {27, 28, 29, 30, 31, 32, 33, 34, 35};

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
        return plugin.getGuiConfig().size("guild-war", 54);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(viewer.getUniqueId());
        if (guild == null || member == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }

        GuildWar war = plugin.getGuildWarManager().getWarOfGuild(guild);
        boolean canManage = member.getRole().atLeast(GuildRole.VICE);
        int[] record = plugin.getGuildWarManager().getRecord(guild);
        int remaining = plugin.getGuildWarManager().remainingToday(guild);
        int available = 0;
        StringJoiner arenaNames = new StringJoiner("<dark_gray>, </dark_gray>");
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.isAvailable()) {
                available++;
            }
            arenaNames.add((arena.isAvailable() ? "<green>" : arena.isInUse() ? "<red>" : "<gray>") + arena.getName());
        }
        if (arenaNames.length() == 0) {
            arenaNames.add("<red>未设置</red>");
        }

        GuiConfig config = plugin.getGuiConfig();
        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .putRaw("war_status", plugin.getGuildWarManager().statusText(guild))
                .put("queue_size", plugin.getGuildWarManager().queueSize())
                .putRaw("daily_left", remaining < 0 ? "<green>无限制</green>"
                        : (remaining > 0 ? "<yellow>" + remaining + "</yellow>" : "<red>已用完</red>"))
                .put("daily_used", plugin.getGuildWarManager().usedToday(guild))
                .put("daily_limit", plugin.getConfigManager().guildWarDailyLimit())
                .put("prepare_seconds", plugin.getConfigManager().guildWarPrepareSeconds())
                .put("battle_seconds", plugin.getConfigManager().guildWarBattleSeconds())
                .put("online_members", plugin.getGuildWarManager().onlineCount(guild))
                .put("participants", plugin.getGuildWarManager().participantCount(guild))
                .put("alive", plugin.getGuildWarManager().aliveCount(guild))
                .put("max_per_side", plugin.getConfigManager().guildWarMaxPerSide())
                .put("total_arenas", plugin.getArenaManager().size())
                .put("available_arenas", available)
                .putRaw("arenas", arenaNames.toString())
                .put("wins", record[0])
                .put("loses", record[1])
                .put("draws", record[2])
                .put("win_contribution", TimeUtil.money(plugin.getConfigManager().guildWarWinReward().contribution()))
                .put("win_funds", TimeUtil.money(plugin.getConfigManager().guildWarWinReward().guildFunds()))
                .put("lose_contribution", TimeUtil.money(plugin.getConfigManager().guildWarLoseReward().contribution()))
                .put("kill_contribution", TimeUtil.money(plugin.getConfigManager().guildWarPerKillReward().contribution()))
                .putRaw("enemy", war == null ? "<gray>无</gray>" : war.getOpponent(guild).getName())
                .putRaw("state", war == null ? "<gray>无进行中的战斗</gray>" : war.getState().name());

        button("guild-war", "status", GuiUtil.def(4, "DIAMOND_SWORD", "<red>匹配公会战</red>",
                "<gray>当前状态: {war_status}</gray>"), placeholders, null);

        button("guild-war", "arena", GuiUtil.def(19, "GRASS_BLOCK", "<green>公会战场地</green>",
                        "<gray>可用: <yellow>{available_arenas}</yellow>/<yellow>{total_arenas}</yellow></gray>"),
                placeholders, event -> plugin.getGuiManager().openWarArena(viewer, 1));

        button("guild-war", "rewards", GuiUtil.def(21, "GOLD_INGOT", "<gold>奖励预览</gold>",
                        "<gray>胜利: <yellow>{win_contribution}</yellow> 贡献</gray>"),
                placeholders, event -> plugin.getGuiManager().openWarReward(viewer));

        button("guild-war", "record", GuiUtil.def(23, "WRITABLE_BOOK", "<aqua>公会战绩</aqua>",
                        "<green>胜 <yellow>{wins}</yellow></green>",
                        "<red>负 <yellow>{loses}</yellow></red>",
                        "<gray>平 <yellow>{draws}</yellow></gray>"),
                placeholders, null);

        button("guild-war", "population", GuiUtil.def(25, "PLAYER_HEAD", "<yellow>参战人口</yellow>",
                        "<gray>在线成员: <yellow>{online_members}</yellow></gray>",
                        "<gray>参战人数: <yellow>{participants}</yellow></gray>",
                        "<gray>存活人数: <yellow>{alive}</yellow></gray>"),
                placeholders, null);

        // 阵容展示
        if (war != null) {
            fillRoster(war, guild, ALLY_SLOTS, true);
            fillRoster(war, guild, ENEMY_SLOTS, false);
        } else {
            fillOnlineMembers(guild, ALLY_SLOTS);
            fillEmptySlots(ENEMY_SLOTS, 0);
        }

        boolean queued = plugin.getGuildWarManager().isQueued(guild);
        if (!queued && war == null) {
            button("guild-war", "match", GuiUtil.def(38, "LIME_CONCRETE", "<green>开始匹配</green>",
                            "<gray>加入匹配队列</gray>"),
                    placeholders, event -> {
                        if (!canManage) {
                            sendNoPermission();
                        } else {
                            plugin.getGuildWarManager().enqueue(guild, viewer);
                        }
                        refresh();
                    });
        }

        if (queued) {
            button("guild-war", "cancel", GuiUtil.def(40, "RED_CONCRETE", "<red>取消匹配</red>",
                            "<gray>把公会移出匹配队列</gray>"),
                    placeholders, event -> {
                        if (!canManage) {
                            sendNoPermission();
                        } else {
                            plugin.getGuildWarManager().dequeue(guild, viewer);
                        }
                        refresh();
                    });
        }

        if (war != null) {
            button("guild-war", "leave", GuiUtil.def(42, "IRON_DOOR", "<yellow>离开战斗</yellow>",
                            "<gray>准备阶段可无损失退出</gray>"),
                    placeholders, event -> {
                        plugin.getGuildWarManager().leave(viewer);
                        refresh();
                    });
        }

        addBackButton(() -> plugin.getGuiManager().openMain(viewer));
        addCloseButton();
        fillEmpty();
    }

    private void sendNoPermission() {
        plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
    }

    /** 战斗中: 展示某一方的参战成员。 */
    private void fillRoster(GuildWar war, Guild guild, int[] slots, boolean ally) {
        GuildWar.Side side = war.getSide(guild);
        if (side == null) {
            return;
        }
        if (!ally) {
            side = side.opponent();
        }
        List<UUID> participants = new ArrayList<>(war.getParticipants(side));
        participants.sort(Comparator
                .comparing((UUID uuid) -> !war.isAlive(uuid))
                .thenComparing(uuid -> -war.getKills(uuid)));

        GuiConfig.GuiItemDef template = plugin.getGuiConfig().template(
                ally ? "guild-war.ally-item" : "guild-war.enemy-item",
                GuiUtil.def(ally ? "PLAYER_HEAD" : "PLAYER_HEAD", "<white>{player}</white> {alive}",
                        "<gray>击杀: <yellow>{kills}</yellow></gray>"));

        for (int index = 0; index < slots.length && index < participants.size(); index++) {
            UUID uuid = participants.get(index);
            GuildMember guildMember = plugin.getGuildManager().getMember(uuid);
            boolean alive = war.isAlive(uuid);
            Placeholders placeholders = Placeholders.of()
                    .put("player", guildMember == null ? plugin.getGuildManager().resolveName(uuid) : guildMember.getName())
                    .putRaw("alive", alive ? "<green>存活</green>" : "<red>淘汰</red>")
                    .put("kills", war.getKills(uuid))
                    .putRaw("role", guildMember == null ? "" : plugin.getConfigManager().roleDisplay(guildMember.getRole()))
                    .putRaw("side", ally ? "<red>我方</red>" : "<blue>敌方</blue>");
            setItem(slots[index], applyHead(template.build(placeholders), uuid), null);
        }
        fillEmptySlots(slots, participants.size());
    }

    /** 未参战时: 展示公会当前在线、可参战的成员。 */
    private void fillOnlineMembers(Guild guild, int[] slots) {
        List<Player> online = plugin.getGuildWarManager().onlineMembers(guild);
        online.sort(Comparator.comparingInt((Player player) -> {
            GuildMember guildMember = plugin.getGuildManager().getMember(player.getUniqueId());
            return guildMember == null ? 0 : -guildMember.getRole().weight();
        }));
        GuiConfig.GuiItemDef template = plugin.getGuiConfig().template("guild-war.member-item",
                GuiUtil.def("PLAYER_HEAD", "<white>{player}</white> <green>可参战</green>",
                        "<gray>职位: {role}</gray>"));
        for (int index = 0; index < slots.length && index < online.size(); index++) {
            Player player = online.get(index);
            GuildMember guildMember = plugin.getGuildManager().getMember(player.getUniqueId());
            Placeholders placeholders = Placeholders.of()
                    .put("player", player.getName())
                    .putRaw("role", guildMember == null ? "" : plugin.getConfigManager().roleDisplay(guildMember.getRole()))
                    .put("contribution", guildMember == null ? "0" : TimeUtil.money(guildMember.getContribution()));
            setItem(slots[index], applyHead(template.build(placeholders), player.getUniqueId()), null);
        }
        fillEmptySlots(slots, online.size());
    }

    private void fillEmptySlots(int[] slots, int from) {
        GuiConfig.GuiItemDef empty = plugin.getGuiConfig().template("guild-war.empty-item", null);
        for (int index = from; index < slots.length; index++) {
            setItem(slots[index], empty != null ? empty.build()
                    : cn.zhm.guild.util.ItemBuilder.of(org.bukkit.Material.GRAY_STAINED_GLASS_PANE)
                    .name("<dark_gray>空位</dark_gray>").hideAll().build(), null);
        }
    }
}
