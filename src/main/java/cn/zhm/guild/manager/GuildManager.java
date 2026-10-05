package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.config.LevelDef;
import cn.zhm.guild.model.ApplicationType;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * 公会核心管理器: 内存缓存 + 异步持久化 + 业务逻辑。
 */
public class GuildManager {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9_\\-]+$");
    private static final Pattern LEGACY_CODE = Pattern.compile("[&\u00A7][0-9a-fk-orA-FK-OR]");

    private final ZHMguildPlugin plugin;

    private final Map<Integer, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<String, Guild> nameIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Guild> playerIndex = new ConcurrentHashMap<>();
    private final Map<UUID, GuildMember> memberIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Long> createCooldown = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ExecutorService executor;

    public GuildManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ZHMguild-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ---------------------------------------------------------
    // 载入 / 保存
    // ---------------------------------------------------------

    /** 从数据库载入全部数据(阻塞)。 */
    public void loadAll() {
        guilds.clear();
        nameIndex.clear();
        playerIndex.clear();
        memberIndex.clear();

        List<Guild> loaded = plugin.getStorage().loadGuilds();
        Map<Integer, List<GuildMember>> members = plugin.getStorage().loadMembers();
        List<GuildApplication> applications = plugin.getStorage().loadApplications();

        int maxId = 0;
        for (Guild guild : loaded) {
            guilds.put(guild.getId(), guild);
            nameIndex.put(guild.getNameLower(), guild);
            if (guild.getLeader() != null) {
                playerIndex.put(guild.getLeader(), guild);
            }
            maxId = Math.max(maxId, guild.getId());
            List<GuildMember> guildMembers = members.get(guild.getId());
            if (guildMembers != null) {
                for (GuildMember member : guildMembers) {
                    guild.addMember(member);
                    memberIndex.put(member.getUuid(), member);
                    if (member.getRole() == GuildRole.LEADER) {
                        guild.setLeader(member.getUuid());
                        guild.setLeaderName(member.getName());
                        playerIndex.put(member.getUuid(), guild);
                    }
                }
            }
        }
        for (GuildApplication application : applications) {
            Guild guild = guilds.get(application.getGuildId());
            if (guild != null) {
                guild.addApplication(application);
            }
        }
        nextId.set(maxId + 1);
        plugin.getLogger().info("已载入 " + guilds.size() + " 个公会, "
                + memberIndex.size() + " 名成员, " + applications.size() + " 条入会申请.");
    }

    /** 提交一个数据库写任务(单线程串行执行, 保证顺序)。 */
    public void submit(Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    plugin.getLogger().log(Level.SEVERE, "数据库写入任务执行失败", throwable);
                }
            });
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "无法提交数据库任务", throwable);
        }
    }

    /** 全量保存(阻塞, 用于关服)。 */
    public void saveAllBlocking() {
        try {
            plugin.getStorage().saveAll(new ArrayList<>(guilds.values()));
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "保存全部公会数据失败", throwable);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void saveGuild(Guild guild) {
        if (guild == null) {
            return;
        }
        submit(() -> plugin.getStorage().updateGuild(guild));
    }

    public void saveMember(Guild guild, GuildMember member) {
        if (guild == null || member == null) {
            return;
        }
        submit(() -> plugin.getStorage().upsertMember(guild.getId(), member));
    }

    public void saveApplication(GuildApplication application) {
        if (application == null) {
            return;
        }
        submit(() -> plugin.getStorage().upsertApplication(application));
    }

    // ---------------------------------------------------------
    // 查询
    // ---------------------------------------------------------

    public Collection<Guild> getGuilds() {
        return guilds.values();
    }

    public Guild getGuild(int id) {
        return guilds.get(id);
    }

    public Guild getGuildByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Guild guild = nameIndex.get(name.toLowerCase());
        if (guild != null) {
            return guild;
        }
        // 兼容带颜色代码的公会名查询
        String plain = Text.strip(LEGACY_CODE.matcher(name).replaceAll(""));
        return nameIndex.get(plain.toLowerCase());
    }

    public Guild getGuildByPlayer(UUID uuid) {
        return uuid == null ? null : playerIndex.get(uuid);
    }

    public Guild getGuildByPlayer(Player player) {
        return player == null ? null : getGuildByPlayer(player.getUniqueId());
    }

    public GuildMember getMember(UUID uuid) {
        return uuid == null ? null : memberIndex.get(uuid);
    }

    public GuildMember getMember(Player player) {
        return player == null ? null : getMember(player.getUniqueId());
    }

    /** 按玩家名查找成员(不区分大小写)。 */
    public GuildMember getMemberByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GuildMember member : memberIndex.values()) {
            if (member.getName().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    /** 按玩家名查找其所在公会。 */
    public Guild getGuildByMemberName(String name) {
        GuildMember member = getMemberByName(name);
        return member == null ? null : getGuildByPlayer(member.getUuid());
    }

    public boolean hasGuild(UUID uuid) {
        return playerIndex.containsKey(uuid);
    }

    /** 公会改名后同步索引。 */
    public void renameIndex(Guild guild, String oldName) {
        if (oldName != null) {
            nameIndex.remove(oldName.toLowerCase());
        }
        nameIndex.put(guild.getNameLower(), guild);
    }

    public int getGuildCount() {
        return guilds.size();
    }

    public int maxMembers(Guild guild) {
        if (guild == null) {
            return plugin.getConfigManager().defaultMaxMembers();
        }
        return plugin.getConfigManager().maxMembersFor(guild.getLevel());
    }

    public List<Guild> sortedGuilds(SortType type) {
        List<Guild> list = new ArrayList<>(guilds.values());
        list.sort((type == null ? SortType.LEVEL : type).comparator());
        return list;
    }

    public int rank(Guild guild, SortType type) {
        if (guild == null) {
            return 0;
        }
        List<Guild> list = sortedGuilds(type);
        for (int index = 0; index < list.size(); index++) {
            if (list.get(index).getId() == guild.getId()) {
                return index + 1;
            }
        }
        return 0;
    }

    /** 某玩家已提交的申请数量。 */
    public int countApplications(UUID uuid) {
        int count = 0;
        for (Guild guild : guilds.values()) {
            if (guild.getApplication(uuid) != null) {
                count++;
            }
        }
        return count;
    }

    /** 某玩家已提交的申请(所在公会)。 */
    public List<Guild> guildsAppliedTo(UUID uuid) {
        List<Guild> result = new ArrayList<>();
        for (Guild guild : guilds.values()) {
            if (guild.getApplication(uuid) != null) {
                result.add(guild);
            }
        }
        return result;
    }

    /** 某玩家收到的邀请。 */
    public List<Guild> invitesOf(UUID uuid) {
        List<Guild> result = new ArrayList<>();
        for (Guild guild : guilds.values()) {
            GuildApplication application = guild.getApplication(uuid);
            if (application != null && application.getType() == ApplicationType.INVITE) {
                result.add(guild);
            }
        }
        return result;
    }

    // ---------------------------------------------------------
    // 名称校验
    // ---------------------------------------------------------

    public String plainName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = Text.strip(raw);
        name = LEGACY_CODE.matcher(name).replaceAll("");
        return name.trim();
    }

    public boolean isValidName(String raw, CommandSender feedback) {
        String plain = plainName(raw);
        int min = plugin.getConfigManager().nameMinLength();
        int max = plugin.getConfigManager().nameMaxLength();
        if (plain.length() < min || plain.length() > max) {
            plugin.getMessages().send(feedback, "common.guild-name-invalid", Placeholders.of()
                    .put("min", min).put("max", max));
            return false;
        }
        if (!NAME_PATTERN.matcher(plain).matches()) {
            plugin.getMessages().send(feedback, "common.guild-name-illegal");
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------
    // 创建 / 解散
    // ---------------------------------------------------------

    /** 创建公会(玩家自行创建, 需要扣费)。成功返回公会对象。 */
    public Guild createGuild(Player player, String rawName) {
        if (player == null) {
            return null;
        }
        return createGuild(player.getUniqueId(), player.getName(), rawName, player, player);
    }

    /**
     * 创建公会核心逻辑。
     *
     * @param owner    公会创始人 UUID
     * @param ownerName 创始人名称
     * @param rawName  公会名称
     * @param payer    承担费用的玩家, 为 null 表示不扣费(管理员创建/离线创建)
     * @param feedback 消息接收者
     */
    public Guild createGuild(UUID owner, String ownerName, String rawName, Player payer, CommandSender feedback) {
        if (owner == null) {
            return null;
        }
        if (hasGuild(owner)) {
            plugin.getMessages().send(feedback, "common.already-in-guild", Placeholders.of()
                    .put("guild", getGuildByPlayer(owner).getName()));
            return null;
        }
        if (!isValidName(rawName, feedback)) {
            return null;
        }
        // 未授权颜色的玩家, 公会名中的格式标签会被清除, 避免注入
        boolean colorAllowed = plugin.getConfigManager().allowColorName()
                && (payer == null || payer.hasPermission("zhmguild.create.color"));
        String name = colorAllowed ? rawName.trim() : plainName(rawName);
        if (getGuildByName(plainName(name)) != null) {
            plugin.getMessages().send(feedback, "common.guild-name-taken", Placeholders.of()
                    .put("guild", plainName(name)));
            return null;
        }
        long now = System.currentTimeMillis();
        long cooldown = createCooldown.getOrDefault(owner, 0L);
        int cooldownSeconds = plugin.getConfigManager().createCooldown();
        if (payer != null && cooldown > now) {
            plugin.getMessages().send(feedback, "common.cooldown", Placeholders.of()
                    .put("time", (cooldown - now) / 1000 + 1));
            return null;
        }
        if (payer != null && !chargeCreateCost(payer)) {
            return null;
        }

        Guild guild = new Guild(nextId.getAndIncrement(), name);
        guild.setLeader(owner);
        guild.setLeaderName(ownerName);
        guild.setCreateTime(now);
        guild.setLevel(1);
        guild.setIcon("WHITE_BANNER");

        GuildMember member = new GuildMember(owner, ownerName, GuildRole.LEADER, 0.0D, now);
        guild.addMember(member);

        guilds.put(guild.getId(), guild);
        nameIndex.put(guild.getNameLower(), guild);
        playerIndex.put(owner, guild);
        memberIndex.put(owner, member);
        if (payer != null && cooldownSeconds > 0) {
            createCooldown.put(owner, now + cooldownSeconds * 1000L);
        }

        submit(() -> {
            if (plugin.getStorage().insertGuild(guild)) {
                plugin.getStorage().upsertMember(guild.getId(), member);
            }
        });

        plugin.getMessages().send(feedback, "create.success", Placeholders.of()
                .put("guild", plainName(name)));
        String broadcastKey = "create.broadcast";
        if (plugin.getMessages().has(broadcastKey)) {
            String broadcast = plugin.getMessages().str(broadcastKey, Placeholders.of()
                    .put("player", ownerName)
                    .put("guild", name));
            Bukkit.getServer().sendMessage(plugin.getMessages().parse(broadcast, feedback));
        }
        return guild;
    }

    private boolean chargeCreateCost(Player player) {
        double money = plugin.getConfigManager().createMoney();
        int points = plugin.getConfigManager().createPoints();
        List<String> items = plugin.getConfigManager().createItems();

        if (money > 0) {
            if (!plugin.getEconomyHook().isEnabled()) {
                if (plugin.getConfigManager().debug()) {
                    plugin.getLogger().info("未安装 Vault, 跳过创建公会的金币消耗。");
                }
            } else {
                if (!plugin.getEconomyHook().has(player, money)) {
                    plugin.getMessages().send(player, "create.need-money", Placeholders.of()
                            .put("amount", TimeUtil.money(money))
                            .put("has", TimeUtil.money(plugin.getEconomyHook().getBalance(player))));
                    return false;
                }
            }
        }
        if (points > 0 && plugin.getPointsHook().isEnabled()) {
            int balance = plugin.getPointsHook().look(player.getUniqueId());
            if (balance < points) {
                plugin.getMessages().send(player, "create.need-points", Placeholders.of()
                        .put("amount", points).put("has", balance));
                return false;
            }
        }
        for (String entry : items) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            Material material = Material.matchMaterial(parts[0].toUpperCase());
            int amount;
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException exception) {
                continue;
            }
            if (material != null && !player.getInventory().contains(material, amount)) {
                plugin.getMessages().send(player, "create.need-item", Placeholders.of()
                        .put("amount", amount).put("item", material.name()));
                return false;
            }
        }

        // 扣费
        if (money > 0 && plugin.getEconomyHook().isEnabled()) {
            plugin.getEconomyHook().withdraw(player, money);
        }
        if (points > 0 && plugin.getPointsHook().isEnabled()) {
            plugin.getPointsHook().take(player.getUniqueId(), points);
        }
        for (String entry : items) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            Material material = Material.matchMaterial(parts[0].toUpperCase());
            if (material == null) {
                continue;
            }
            try {
                player.getInventory().removeItem(new ItemStack(material, Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException ignored) {
                // 忽略
            }
        }
        return true;
    }

    /** 解散公会。 */
    public void dissolveGuild(Guild guild) {
        if (guild == null) {
            return;
        }
        int id = guild.getId();
        guilds.remove(id);
        nameIndex.remove(guild.getNameLower());
        for (UUID uuid : new ArrayList<>(guild.getMemberMap().keySet())) {
            playerIndex.remove(uuid);
            memberIndex.remove(uuid);
        }
        submit(() -> plugin.getStorage().deleteGuild(id));
    }

    // ---------------------------------------------------------
    // 成员
    // ---------------------------------------------------------

    /** 加入公会。 */
    public boolean addMember(Guild guild, UUID uuid, String name, GuildRole role, CommandSender feedback) {
        if (guild == null || uuid == null) {
            return false;
        }
        if (hasGuild(uuid)) {
            plugin.getMessages().send(feedback, "common.already-in-guild", Placeholders.of()
                    .put("guild", getGuildByPlayer(uuid).getName()));
            return false;
        }
        if (guild.getMemberCount() >= maxMembers(guild)) {
            plugin.getMessages().send(feedback, "common.guild-full", Placeholders.of()
                    .put("guild", guild.getName())
                    .put("current", guild.getMemberCount())
                    .put("max", maxMembers(guild)));
            return false;
        }
        GuildMember member = new GuildMember(uuid, name, role, 0.0D, System.currentTimeMillis());
        guild.addMember(member);
        playerIndex.put(uuid, guild);
        memberIndex.put(uuid, member);
        guild.removeApplication(uuid);
        submit(() -> {
            plugin.getStorage().upsertMember(guild.getId(), member);
            plugin.getStorage().deleteApplication(guild.getId(), uuid);
        });
        return true;
    }

    public void removeMember(Guild guild, UUID uuid) {
        if (guild == null || uuid == null) {
            return;
        }
        guild.removeMember(uuid);
        playerIndex.remove(uuid);
        memberIndex.remove(uuid);
        int guildId = guild.getId();
        submit(() -> plugin.getStorage().deleteMember(guildId, uuid));
    }

    // ---------------------------------------------------------
    // 申请
    // ---------------------------------------------------------

    public void addApplication(Guild guild, UUID uuid, String name, ApplicationType type) {
        if (guild == null || uuid == null) {
            return;
        }
        GuildApplication application = new GuildApplication(
                guild.getId(), uuid, name, type, System.currentTimeMillis());
        guild.addApplication(application);
        saveApplication(application);
    }

    public void removeApplication(Guild guild, UUID uuid) {
        if (guild == null || uuid == null) {
            return;
        }
        guild.removeApplication(uuid);
        int guildId = guild.getId();
        submit(() -> plugin.getStorage().deleteApplication(guildId, uuid));
    }

    /** 清理所有过期申请。 */
    public int purgeExpiredApplications() {
        int hours = plugin.getConfigManager().applicationExpireHours();
        if (hours <= 0) {
            return 0;
        }
        long deadline = System.currentTimeMillis() - hours * 3600_000L;
        int removed = 0;
        for (Guild guild : guilds.values()) {
            for (GuildApplication application : new ArrayList<>(guild.getApplications())) {
                if (application.getTime() < deadline) {
                    removeApplication(guild, application.getUuid());
                    removed++;
                }
            }
        }
        return removed;
    }

    // ---------------------------------------------------------
    // 等级 / 资金 / 活跃
    // ---------------------------------------------------------

    public double upMoney(Guild guild) {
        return plugin.getConfigManager().levelDef(guild.getLevel()).upMoney();
    }

    public int upActive(Guild guild) {
        return plugin.getConfigManager().levelDef(guild.getLevel()).upActive();
    }

    public boolean isMaxLevel(Guild guild) {
        return guild.getLevel() >= plugin.getConfigManager().maxLevel();
    }

    /** 升级公会。 */
    public boolean upgrade(Guild guild, CommandSender feedback) {
        if (guild == null) {
            return false;
        }
        if (isMaxLevel(guild)) {
            plugin.getMessages().send(feedback, "up.max-level", Placeholders.of()
                    .put("level", guild.getLevel()));
            return false;
        }
        double money = upMoney(guild);
        int active = upActive(guild);
        if (guild.getFunds() < money) {
            plugin.getMessages().send(feedback, "up.need-money", Placeholders.of()
                    .put("amount", TimeUtil.money(money))
                    .put("has", TimeUtil.money(guild.getFunds())));
            return false;
        }
        if (guild.getActive() < active) {
            plugin.getMessages().send(feedback, "up.need-active", Placeholders.of()
                    .put("amount", active)
                    .put("has", guild.getActive()));
            return false;
        }
        guild.setFunds(guild.getFunds() - money);
        guild.setActive(guild.getActive() - active);
        guild.setLevel(guild.getLevel() + 1);
        saveGuild(guild);

        Placeholders placeholders = Placeholders.of()
                .put("guild", guild.getName())
                .put("level", guild.getLevel());
        plugin.getMessages().send(feedback, "up.success", placeholders);
        String broadcastKey = "up.broadcast";
        if (plugin.getMessages().has(broadcastKey)) {
            Bukkit.getServer().sendMessage(plugin.getMessages().parse(
                    plugin.getMessages().str(broadcastKey, placeholders), feedback));
        }
        return true;
    }

    public void addFunds(Guild guild, double amount) {
        if (guild == null) {
            return;
        }
        guild.addFunds(amount);
        saveGuild(guild);
    }

    public void addActive(Guild guild, int amount) {
        if (guild == null) {
            return;
        }
        guild.addActive(amount);
        guild.addMonthActive(amount);
        saveGuild(guild);
    }

    public void addOre(Guild guild, int amount) {
        if (guild == null) {
            return;
        }
        guild.setOre(guild.getOre() + amount);
        saveGuild(guild);
    }

    // ---------------------------------------------------------
    // 签到
    // ---------------------------------------------------------

    /** 每日签到, 返回奖励描述; 已签到返回 null。 */
    public boolean signIn(Player player, Guild guild, GuildMember member) {
        if (player == null || guild == null || member == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (TimeUtil.sameDay(member.getLastSignIn(), now)) {
            plugin.getMessages().send(player, "signin.already");
            return false;
        }
        int streak = member.getSignStreak();
        long resetMillis = plugin.getConfigManager().signInResetHours() * 3600_000L;
        if (resetMillis > 0 && now - member.getLastSignIn() > resetMillis) {
            streak = 0;
        }
        streak++;
        double multiplier = 1.0D;
        for (Map.Entry<Integer, Double> entry : plugin.getConfigManager().streakBonus().entrySet()) {
            if (streak >= entry.getKey()) {
                multiplier = Math.max(multiplier, entry.getValue());
            }
        }

        double money = plugin.getConfigManager().signInMoney() * multiplier;
        double contribution = plugin.getConfigManager().signInContribution() * multiplier;
        int points = (int) Math.round(plugin.getConfigManager().signInPoints() * multiplier);
        int guildActive = plugin.getConfigManager().signInGuildActive();
        double guildMoney = plugin.getConfigManager().signInGuildMoney();

        member.setLastSignIn(now);
        member.setSignStreak(streak);
        member.addContribution(contribution);
        guild.addFunds(guildMoney);
        guild.addActive(guildActive);
        guild.addMonthActive(guildActive);

        if (money > 0 && plugin.getEconomyHook().isEnabled()) {
            plugin.getEconomyHook().deposit(player, money);
        }
        if (points > 0 && plugin.getPointsHook().isEnabled()) {
            plugin.getPointsHook().give(player.getUniqueId(), points);
        }

        saveMember(guild, member);
        saveGuild(guild);

        plugin.getMessages().send(player, "signin.success", Placeholders.of()
                .put("money", TimeUtil.money(money))
                .put("contribution", TimeUtil.money(contribution))
                .put("active", guildActive));
        if (multiplier > 1.0D) {
            plugin.getMessages().send(player, "signin.streak", Placeholders.of()
                    .put("days", streak)
                    .put("bonus", TimeUtil.money(multiplier)));
        }
        String broadcastKey = "signin.broadcast";
        if (plugin.getMessages().has(broadcastKey)) {
            Bukkit.getServer().sendMessage(plugin.getMessages().parse(
                    plugin.getMessages().str(broadcastKey, Placeholders.of()
                            .put("player", player.getName())
                            .put("days", streak)), player));
        }
        return true;
    }

    // ---------------------------------------------------------
    // 主城
    // ---------------------------------------------------------

    public void setHome(Guild guild, Location location) {
        if (guild == null) {
            return;
        }
        guild.setHome(location == null ? null : location.clone());
        saveGuild(guild);
    }

    // ---------------------------------------------------------
    // 维护
    // ---------------------------------------------------------

    /** 刷新公会数据(修复会长、称号、人数等)。 */
    public void refresh(Guild guild) {
        if (guild == null) {
            return;
        }
        GuildMember leaderMember = guild.findLeaderMember();
        if (leaderMember != null) {
            guild.setLeader(leaderMember.getUuid());
            guild.setLeaderName(leaderMember.getName());
            playerIndex.put(leaderMember.getUuid(), guild);
        }
        for (GuildMember member : guild.getMembers()) {
            memberIndex.put(member.getUuid(), member);
            playerIndex.put(member.getUuid(), guild);
        }
        saveGuild(guild);
    }

    /** 刷新全部公会。 */
    public void refreshAll() {
        for (Guild guild : guilds.values()) {
            refresh(guild);
        }
    }

    /** 每月重置月度活跃。 */
    public void resetMonthActive() {
        for (Guild guild : guilds.values()) {
            guild.setMonthActive(0);
            saveGuild(guild);
        }
        plugin.getLogger().info("已重置全部公会的月度活跃。");
    }

    /** 为在线玩家所在公会累积活跃。 */
    public void tickOnlineActive() {
        int amount = plugin.getConfigManager().activePerMinuteOnline();
        if (amount <= 0) {
            return;
        }
        List<Guild> touched = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Guild guild = getGuildByPlayer(player.getUniqueId());
            if (guild != null && !touched.contains(guild)) {
                touched.add(guild);
            }
        }
        for (Guild guild : touched) {
            guild.addActive(amount);
            guild.addMonthActive(amount);
            saveGuild(guild);
        }
    }

    /** 判断玩家是否有权限操作目标成员。 */
    public boolean canManage(Player operator, Guild guild, GuildMember target) {
        GuildMember self = getMember(operator.getUniqueId());
        if (self == null || guild == null || target == null) {
            return false;
        }
        if (!self.canManage()) {
            return false;
        }
        return self.getRole().above(target.getRole()) || self.isLeader() || self.getRole() == target.getRole() && self.isLeader();
    }

    /** 离线玩家名称查询。 */
    public String resolveName(UUID uuid) {
        GuildMember member = memberIndex.get(uuid);
        if (member != null) {
            return member.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() == null ? uuid.toString().substring(0, 8) : offlinePlayer.getName();
    }
}
