package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.config.WarReward;
import cn.zhm.guild.model.Arena;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildWar;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 匹配公会战管理器。
 *
 * <p>流程: 公会长/副会长发起匹配 → 进入匹配队列 → 队列中达到 2 个公会且有空闲场地时自动配对
 * → 传送到场并进入准备阶段 → 战斗阶段 → 一方全灭或超时后结算 → 发放奖励并传送回原位。</p>
 */
public class GuildWarManager {

    /** 公会战期间的伤害判定结果。 */
    public enum DamageRule {
        /** 强制允许。 */
        ALLOW,
        /** 强制拒绝。 */
        DENY,
        /** 不做判定, 交给普通规则。 */
        NONE
    }

    private final ZHMguildPlugin plugin;
    private final Map<Integer, GuildWar> wars = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerIndex = new ConcurrentHashMap<>();
    /** 匹配队列: 公会 id -> 入队时间, 保持先进先出。 */
    private final Map<Integer, Long> queue = new LinkedHashMap<>();
    private final AtomicInteger nextWarId = new AtomicInteger(1);
    private BukkitTask tickTask;

    public GuildWarManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------
    // 生命周期
    // ---------------------------------------------------------

    public void start() {
        plugin.getArenaManager().releaseAll();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /** 关闭: 结束全部进行中的战斗并把玩家送回原位。 */
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (GuildWar war : new ArrayList<>(wars.values())) {
            teleportBack(war);
            war.getArena().release();
        }
        wars.clear();
        playerIndex.clear();
        queue.clear();
    }

    // ---------------------------------------------------------
    // 查询
    // ---------------------------------------------------------

    public GuildWar getWar(int id) {
        return wars.get(id);
    }

    public GuildWar getWar(UUID uuid) {
        Integer id = uuid == null ? null : playerIndex.get(uuid);
        return id == null ? null : wars.get(id);
    }

    public GuildWar getWarOfGuild(Guild guild) {
        if (guild == null) {
            return null;
        }
        for (GuildWar war : wars.values()) {
            if (war.getRedGuild().getId() == guild.getId() || war.getBlueGuild().getId() == guild.getId()) {
                return war;
            }
        }
        return null;
    }

    public boolean isInWar(UUID uuid) {
        return playerIndex.containsKey(uuid);
    }

    public List<GuildWar> getWars() {
        return new ArrayList<>(wars.values());
    }

    public boolean isQueued(Guild guild) {
        return guild != null && queue.containsKey(guild.getId());
    }

    public int queueSize() {
        return queue.size();
    }

    /** 队列中的公会(按排队顺序)。 */
    public List<Guild> queuedGuilds() {
        List<Guild> list = new ArrayList<>();
        for (Integer id : new ArrayList<>(queue.keySet())) {
            Guild guild = plugin.getGuildManager().getGuild(id);
            if (guild != null) {
                list.add(guild);
            }
        }
        return list;
    }

    /** 公会当前在队列中的位次, 未排队返回 null。 */
    public Integer queuePosition(Guild guild) {
        if (guild == null) {
            return null;
        }
        int index = 1;
        for (Integer id : queue.keySet()) {
            if (id == guild.getId()) {
                return index;
            }
            index++;
        }
        return null;
    }

    /** 供 GUI 显示的状态文本。 */
    public String statusText(Guild guild) {
        if (guild == null) {
            return "<gray>未加入公会</gray>";
        }
        GuildWar war = getWarOfGuild(guild);
        if (war != null) {
            return switch (war.getState()) {
                case PREPARING -> "<yellow>准备中 " + war.remainingSeconds() + " 秒</yellow>";
                case RUNNING -> "<red>战斗中 剩余 " + war.remainingSeconds() + " 秒</red>";
                case ENDED -> "<gray>结算中</gray>";
            };
        }
        Integer position = queuePosition(guild);
        if (position != null) {
            return "<aqua>匹配中 (第 " + position + " 位, 共 " + queue.size() + " 个公会)</aqua>";
        }
        return "<gray>未参战</gray>";
    }

    // ---------------------------------------------------------
    // 匹配队列
    // ---------------------------------------------------------

    /** 公会发起匹配。 */
    public boolean enqueue(Guild guild, CommandSender feedback) {
        return enqueue(guild, feedback, false);
    }

    /**
     * 公会发起匹配。
     *
     * @param ignoreOnlineCheck 管理员强制入队时忽略在线人数检查
     */
    public boolean enqueue(Guild guild, CommandSender feedback, boolean ignoreOnlineCheck) {
        if (!plugin.getConfigManager().guildWarEnabled()) {
            plugin.getMessages().send(feedback, "war.disabled");
            return false;
        }
        if (guild == null) {
            plugin.getMessages().send(feedback, "common.not-in-guild");
            return false;
        }
        if (queue.containsKey(guild.getId())) {
            plugin.getMessages().send(feedback, "war.already-queued");
            return false;
        }
        if (getWarOfGuild(guild) != null) {
            plugin.getMessages().send(feedback, "war.already-fighting");
            return false;
        }
        int limit = plugin.getConfigManager().guildWarDailyLimit();
        int used = usedToday(guild);
        if (limit > 0 && used >= limit) {
            plugin.getMessages().send(feedback, "war.daily-limit", Placeholders.of()
                    .put("limit", limit)
                    .put("used", used));
            return false;
        }
        if (!ignoreOnlineCheck && onlineMembers(guild).isEmpty()) {
            plugin.getMessages().send(feedback, "war.no-online-members");
            return false;
        }
        queue.put(guild.getId(), System.currentTimeMillis());
        broadcastGuild(guild, "war.queued", Placeholders.of()
                .put("guild", guild.getName())
                .put("count", queue.size()));
        if (plugin.getArenaManager().findAvailable() == null) {
            plugin.getMessages().send(feedback, "war.no-arena-wait");
        }
        return true;
    }

    /** 公会取消匹配。 */
    public boolean dequeue(Guild guild, CommandSender feedback) {
        if (guild == null) {
            plugin.getMessages().send(feedback, "common.not-in-guild");
            return false;
        }
        if (queue.remove(guild.getId()) == null) {
            plugin.getMessages().send(feedback, "war.not-queued");
            return false;
        }
        broadcastGuild(guild, "war.cancelled", Placeholders.of().put("guild", guild.getName()));
        return true;
    }

    // ---------------------------------------------------------
    // 匹配与开战
    // ---------------------------------------------------------

    private void tick() {
        try {
            tryMatch();
            tickWars();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("公会战心跳异常: " + throwable);
        }
    }

    private void tryMatch() {
        if (!plugin.getConfigManager().guildWarEnabled()) {
            return;
        }
        while (queue.size() >= 2) {
            Arena arena = plugin.getArenaManager().findAvailable();
            if (arena == null) {
                return;
            }
            Iterator<Integer> iterator = queue.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            int firstId = iterator.next();
            iterator.remove();
            if (!iterator.hasNext()) {
                // 只剩一个, 放回去等待下一个公会
                queue.put(firstId, System.currentTimeMillis());
                return;
            }
            int secondId = iterator.next();
            iterator.remove();

            Guild first = plugin.getGuildManager().getGuild(firstId);
            Guild second = plugin.getGuildManager().getGuild(secondId);
            if (first == null || second == null) {
                continue;
            }
            if (startWar(first, second, arena, null) == null) {
                // 开战失败(例如双方无人在线), 不重新入队以免死循环
                plugin.getLogger().info("公会战匹配失败: " + first.getName() + " vs " + second.getName());
            }
        }
    }

    private void tickWars() {
        for (GuildWar war : new ArrayList<>(wars.values())) {
            switch (war.getState()) {
                case PREPARING -> {
                    long left = war.remainingSeconds();
                    if (left <= 0) {
                        beginBattle(war);
                    } else if (left <= 5 || left % 10 == 0) {
                        broadcastWar(war, "war.prepare-countdown", Placeholders.of().put("time", left));
                    }
                }
                case RUNNING -> {
                    int limit = plugin.getConfigManager().guildWarBattleSeconds();
                    if (limit > 0) {
                        long elapsed = (System.currentTimeMillis() - war.getBattleStartTime()) / 1000L;
                        if (elapsed >= limit) {
                            broadcastWar(war, "war.time-up", null);
                            endWar(war);
                        }
                    }
                }
                default -> {
                    // ENDED 状态无需处理
                }
            }
        }
    }

    /**
     * 开始一场公会战。
     *
     * @return 成功返回战斗实例, 失败返回 null
     */
    public GuildWar startWar(Guild red, Guild blue, Arena arena, CommandSender feedback) {
        if (red == null || blue == null || red.getId() == blue.getId()) {
            return null;
        }
        if (arena == null || !arena.isAvailable()) {
            plugin.getMessages().send(feedback, "war.no-arena");
            return null;
        }
        GuildWar war = new GuildWar(nextWarId.getAndIncrement(), arena, red, blue);
        arena.occupy(war.getId());
        wars.put(war.getId(), war);

        int max = plugin.getConfigManager().guildWarMaxPerSide();
        enroll(war, red, GuildWar.Side.RED, arena.getRedSpawn(), max);
        enroll(war, blue, GuildWar.Side.BLUE, arena.getBlueSpawn(), max);

        if (war.participantCount(GuildWar.Side.RED) == 0 || war.participantCount(GuildWar.Side.BLUE) == 0) {
            plugin.getMessages().send(feedback, "war.one-side-empty", Placeholders.of()
                    .put("guild", war.participantCount(GuildWar.Side.RED) == 0 ? red.getName() : blue.getName()));
            for (UUID uuid : new ArrayList<>(war.getParticipants())) {
                Player player = Bukkit.getPlayer(uuid);
                Location back = war.getReturnLocation(uuid);
                if (player != null && back != null) {
                    player.teleport(back);
                }
            }
            cleanup(war);
            return null;
        }

        consumeDaily(red);
        consumeDaily(blue);
        war.setPhaseEndTime(System.currentTimeMillis() + plugin.getConfigManager().guildWarPrepareSeconds() * 1000L);
        broadcastWar(war, "war.started", Placeholders.of()
                .put("red", red.getName())
                .put("blue", blue.getName())
                .put("arena", arena.getName())
                .put("time", plugin.getConfigManager().guildWarPrepareSeconds()));
        return war;
    }

    private void enroll(GuildWar war, Guild guild, GuildWar.Side side, Location spawn, int max) {
        List<GuildMember> members = guild.getSortedMembers();
        int count = 0;
        for (GuildMember member : members) {
            if (count >= max) {
                break;
            }
            Player player = Bukkit.getPlayer(member.getUuid());
            if (player == null) {
                continue;
            }
            war.addParticipant(member.getUuid(), side);
            war.setReturnLocation(member.getUuid(), player.getLocation());
            playerIndex.put(member.getUuid(), war.getId());
            count++;
            if (spawn != null) {
                player.teleport(spawn);
            }
            if (plugin.getConfigManager().guildWarDisableFly()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            plugin.getMessages().send(player, "war.joined", Placeholders.of()
                    .put("guild", guild.getName())
                    .put("side", side == GuildWar.Side.RED ? "红队" : "蓝队")
                    .put("enemy", war.getOpponent(guild).getName())
                    .put("arena", war.getArena().getName()));
        }
    }

    private void beginBattle(GuildWar war) {
        war.toRunning();
        war.setPhaseEndTime(System.currentTimeMillis() + plugin.getConfigManager().guildWarBattleSeconds() * 1000L);
        broadcastWar(war, "war.battle-start", Placeholders.of()
                .put("red", war.getRedGuild().getName())
                .put("blue", war.getBlueGuild().getName()));
    }

    // ---------------------------------------------------------
    // 战斗过程
    // ---------------------------------------------------------

    /** 参战玩家被击杀。 */
    public void eliminate(Player victim, Player killer) {
        GuildWar war = getWar(victim.getUniqueId());
        if (war == null || war.getState() != GuildWar.State.RUNNING || !war.isAlive(victim.getUniqueId())) {
            return;
        }
        war.eliminate(victim.getUniqueId());
        if (killer != null && war.isParticipant(killer.getUniqueId())
                && war.getSide(killer.getUniqueId()) != war.getSide(victim.getUniqueId())) {
            war.addKill(killer.getUniqueId());
        }
        Guild guild = guildOf(war, victim.getUniqueId());
        broadcastWar(war, "war.eliminated", Placeholders.of()
                .put("player", victim.getName())
                .put("guild", guild == null ? "?" : guild.getName())
                .put("killer", killer == null ? "环境" : killer.getName())
                .put("alive", war.aliveCount(war.getSide(victim.getUniqueId()))));
        checkEnd(war);
    }

    private void checkEnd(GuildWar war) {
        if (war.getState() != GuildWar.State.RUNNING) {
            return;
        }
        if (war.aliveCount(GuildWar.Side.RED) == 0 || war.aliveCount(GuildWar.Side.BLUE) == 0) {
            endWar(war);
        }
    }

    /** 玩家主动退出 / 掉线。 */
    public void handleQuit(Player player) {
        GuildWar war = getWar(player.getUniqueId());
        if (war == null) {
            return;
        }
        if (war.getState() == GuildWar.State.RUNNING) {
            if (war.isAlive(player.getUniqueId())) {
                war.eliminate(player.getUniqueId());
                Guild guild = guildOf(war, player.getUniqueId());
                broadcastWar(war, "war.eliminated-quit", Placeholders.of()
                        .put("player", player.getName())
                        .put("guild", guild == null ? "?" : guild.getName()));
                checkEnd(war);
            }
            return;
        }
        if (war.getState() == GuildWar.State.PREPARING) {
            removeFromPrepare(war, player.getUniqueId());
        }
    }

    private void removeFromPrepare(GuildWar war, UUID uuid) {
        war.removeParticipant(uuid);
        playerIndex.remove(uuid);
        if (war.participantCount(GuildWar.Side.RED) == 0 || war.participantCount(GuildWar.Side.BLUE) == 0) {
            broadcastWar(war, "war.cancelled-empty", null);
            teleportBack(war);
            cleanup(war);
        }
    }

    /** 玩家主动离开战斗。 */
    public boolean leave(Player player) {
        GuildWar war = getWar(player.getUniqueId());
        if (war == null) {
            plugin.getMessages().send(player, "war.not-in-war");
            return false;
        }
        switch (war.getState()) {
            case PREPARING -> {
                Location back = war.getReturnLocation(player.getUniqueId());
                removeFromPrepare(war, player.getUniqueId());
                if (back != null) {
                    player.teleport(back);
                }
                plugin.getMessages().send(player, "war.left");
                return true;
            }
            case RUNNING -> {
                if (!war.isAlive(player.getUniqueId())) {
                    plugin.getMessages().send(player, "war.already-eliminated");
                    return false;
                }
                handleQuit(player);
                plugin.getMessages().send(player, "war.left-running");
                return true;
            }
            default -> {
                plugin.getMessages().send(player, "war.not-in-war");
                return false;
            }
        }
    }

    // ---------------------------------------------------------
    // 结束与结算
    // ---------------------------------------------------------

    /** 结束一场公会战并结算。 */
    public void endWar(GuildWar war) {
        if (war == null || war.getState() == GuildWar.State.ENDED) {
            return;
        }
        Guild winner = war.judge();
        war.setWinnerName(winner == null ? null : winner.getName());
        war.toEnded();

        Guild red = war.getRedGuild();
        Guild blue = war.getBlueGuild();
        WarReward win = plugin.getConfigManager().guildWarWinReward();
        WarReward lose = plugin.getConfigManager().guildWarLoseReward();
        WarReward draw = plugin.getConfigManager().guildWarDrawReward();

        if (winner == null) {
            giveReward(war, red, draw, false);
            giveReward(war, blue, draw, false);
        } else {
            Guild loser = winner.getId() == red.getId() ? blue : red;
            giveReward(war, winner, win, true);
            giveReward(war, loser, lose, false);
        }

        broadcastWar(war, winner == null ? "war.result-draw" : "war.result-win", Placeholders.of()
                .put("winner", winner == null ? "无" : winner.getName())
                .put("red", red.getName())
                .put("blue", blue.getName())
                .put("red_kills", war.getKills(GuildWar.Side.RED))
                .put("blue_kills", war.getKills(GuildWar.Side.BLUE))
                .put("red_alive", war.aliveCount(GuildWar.Side.RED))
                .put("blue_alive", war.aliveCount(GuildWar.Side.BLUE)));

        teleportBack(war);
        cleanup(war);
    }

    /** 管理员强制结束。 */
    public boolean stopWar(GuildWar war, CommandSender feedback) {
        if (war == null) {
            plugin.getMessages().send(feedback, "war.none-running");
            return false;
        }
        broadcastWar(war, "war.force-stopped", null);
        endWar(war);
        return true;
    }

    public int stopAll(CommandSender feedback) {
        int count = wars.size();
        for (GuildWar war : new ArrayList<>(wars.values())) {
            broadcastWar(war, "war.force-stopped", null);
            endWar(war);
        }
        if (count == 0) {
            plugin.getMessages().send(feedback, "war.none-running");
        }
        return count;
    }

    private void giveReward(GuildWar war, Guild guild, WarReward reward, boolean winner) {
        if (guild == null || reward == null) {
            return;
        }
        if (reward.guildFunds() > 0 || reward.guildActive() > 0) {
            guild.addFunds(reward.guildFunds());
            guild.addActive(reward.guildActive());
            guild.addMonthActive(reward.guildActive());
            plugin.getGuildManager().saveGuild(guild);
        }
        GuildWar.Side side = war.getSide(guild);
        if (side == null) {
            return;
        }
        double perKill = plugin.getConfigManager().guildWarPerKillReward().contribution();
        for (UUID uuid : war.getParticipants(side)) {
            GuildMember member = plugin.getGuildManager().getMember(uuid);
            int kills = war.getKills(uuid);
            double total = reward.contribution() + perKill * kills;
            if (member != null && total > 0) {
                member.addContribution(total);
                plugin.getGuildManager().saveMember(guild, member);
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                plugin.getMessages().send(online, winner ? "war.reward-win" : "war.reward-lose", Placeholders.of()
                        .put("contribution", TimeUtil.money(total))
                        .put("kills", kills)
                        .put("guild", guild.getName()));
            }
            runCommands(reward.commands(), online, guild);
        }
    }

    private void runCommands(List<String> commands, Player player, Guild guild) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw
                    .replace("{player}", player == null ? "" : player.getName())
                    .replace("{guild}", guild == null ? "" : guild.getName());
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Throwable throwable) {
                plugin.getLogger().warning("执行公会战奖励指令失败: " + command + " (" + throwable.getMessage() + ")");
            }
        }
    }

    private void teleportBack(GuildWar war) {
        for (UUID uuid : new ArrayList<>(war.getParticipants())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Location back = war.getReturnLocation(uuid);
                if (back != null) {
                    player.teleport(back);
                }
                plugin.getMessages().send(player, "war.returned");
            }
            playerIndex.remove(uuid);
        }
    }

    private void cleanup(GuildWar war) {
        war.getArena().release();
        wars.remove(war.getId());
        for (UUID uuid : new ArrayList<>(war.getParticipants())) {
            playerIndex.remove(uuid);
        }
    }

    /** 取消一场尚未开始的战斗。 */
    public void cancelWar(GuildWar war, String messagePath) {
        if (war == null) {
            return;
        }
        broadcastWar(war, messagePath, null);
        teleportBack(war);
        cleanup(war);
    }

    // ---------------------------------------------------------
    // 伤害判定
    // ---------------------------------------------------------

    /**
     * 公会战期间的 PVP 判定。
     * <ul>
     *   <li>双方都是同一场战斗的参战者且阵营不同 → 允许(仅战斗阶段)</li>
     *   <li>只有一方是参战者 → 拒绝, 保护非参战玩家</li>
     *   <li>同一阵营 → 交给普通规则(默认同公会免伤)</li>
     * </ul>
     */
    public DamageRule damageRule(Player attacker, Player victim) {
        GuildWar attackerWar = getWar(attacker.getUniqueId());
        GuildWar victimWar = getWar(victim.getUniqueId());
        if (attackerWar == null && victimWar == null) {
            return DamageRule.NONE;
        }
        if (attackerWar == null || victimWar == null || attackerWar.getId() != victimWar.getId()) {
            return DamageRule.DENY;
        }
        GuildWar.Side attackerSide = attackerWar.getSide(attacker.getUniqueId());
        GuildWar.Side victimSide = attackerWar.getSide(victim.getUniqueId());
        if (attackerSide == null || victimSide == null || attackerSide == victimSide) {
            return DamageRule.NONE;
        }
        if (attackerWar.getState() != GuildWar.State.RUNNING) {
            return DamageRule.DENY;
        }
        if (!attackerWar.isAlive(attacker.getUniqueId()) || !attackerWar.isAlive(victim.getUniqueId())) {
            return DamageRule.DENY;
        }
        return DamageRule.ALLOW;
    }

    // ---------------------------------------------------------
    // 每日次数
    // ---------------------------------------------------------

    public int usedToday(Guild guild) {
        if (guild == null) {
            return 0;
        }
        String value = plugin.getDataConfig().getString("war-daily." + guild.getId(), "");
        String today = LocalDate.now().toString();
        if (value.startsWith(today + ":")) {
            try {
                return Integer.parseInt(value.substring(today.length() + 1));
            } catch (NumberFormatException exception) {
                return 0;
            }
        }
        return 0;
    }

    public int remainingToday(Guild guild) {
        int limit = plugin.getConfigManager().guildWarDailyLimit();
        if (limit <= 0) {
            return -1;
        }
        return Math.max(0, limit - usedToday(guild));
    }

    private void consumeDaily(Guild guild) {
        if (guild == null) {
            return;
        }
        plugin.getDataConfig().set("war-daily." + guild.getId(), LocalDate.now() + ":" + (usedToday(guild) + 1));
        plugin.saveData();
    }

    // ---------------------------------------------------------
    // 工具
    // ---------------------------------------------------------

    public Guild guildOf(GuildWar war, UUID uuid) {
        GuildWar.Side side = war.getSide(uuid);
        return side == null ? null : war.getGuild(side);
    }

    /** 公会当前在线的成员。 */
    public List<Player> onlineMembers(Guild guild) {
        List<Player> list = new ArrayList<>();
        if (guild == null) {
            return list;
        }
        for (GuildMember member : guild.getMembers()) {
            Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    private void broadcastGuild(Guild guild, String path, Placeholders placeholders) {
        if (guild == null || !plugin.getMessages().has(path)) {
            return;
        }
        String raw = plugin.getMessages().str(path, placeholders);
        for (Player player : onlineMembers(guild)) {
            player.sendMessage(plugin.getMessages().parse(raw, player));
        }
    }

    private void broadcastWar(GuildWar war, String path, Placeholders placeholders) {
        if (war == null || !plugin.getMessages().has(path)) {
            return;
        }
        String raw = plugin.getMessages().str(path, placeholders);
        for (UUID uuid : war.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(plugin.getMessages().parse(raw, player));
            }
        }
    }
}
