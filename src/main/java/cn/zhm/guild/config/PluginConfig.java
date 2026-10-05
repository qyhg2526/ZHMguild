package cn.zhm.guild.config;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.GuildRole;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config.yml 读取器。所有字段都带默认值, 配置缺项不会导致报错。
 */
public class PluginConfig {

    private final ZHMguildPlugin plugin;

    // storage
    private String storageType = "SQLITE";
    private String tablePrefix = "zhm_";
    private int autoSaveMinutes = 10;
    private String mysqlHost = "127.0.0.1";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "zhmguild";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";
    private int mysqlPoolSize = 6;
    private boolean mysqlUseSsl = false;
    private String mysqlParameters = "useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
    private String sqliteFile = "zhmguild.db";

    // guild
    private int nameMinLength = 2;
    private int nameMaxLength = 12;
    private boolean allowColorName = false;
    private boolean nameCaseSensitive = false;
    private int maxApplications = 5;
    private int applicationExpireHours = 168;
    private int defaultMaxMembers = 10;
    private double createMoney = 1000.0D;
    private int createPoints = 0;
    private List<String> createItems = new ArrayList<>();
    private int createCooldown = 60;
    private boolean dissolveRefund = false;
    private double renameCost = 5000.0D;
    private double contributeMin = 100.0D;
    private List<Double> contributePresets = List.of(1000.0D, 10000.0D, 100000.0D);

    // levels
    private final Map<Integer, LevelDef> levels = new LinkedHashMap<>();
    private int maxLevel = 6;

    // home
    private int homeTeleportCooldown = 30;
    private int homeTeleportDelay = 3;
    private boolean homeCancelOnMove = true;
    private GuildRole homeSetPermission = GuildRole.VICE;

    // sign in
    private double signInMoney = 200.0D;
    private int signInPoints = 0;
    private double signInContribution = 10.0D;
    private int signInGuildActive = 20;
    private double signInGuildMoney = 0.0D;
    private int signInResetHours = 48;
    private final Map<Integer, Double> streakBonus = new LinkedHashMap<>();

    // chat
    private String chatFormat = "<dark_gray>[公会]</dark_gray> <gray>{role}</gray> <white>{player}</white> <dark_gray>»</dark_gray> <white>{message}</white>";
    private String chatQuickPrefix = "!";
    private boolean chatSpyEnabled = true;

    // active
    private int activePerMinuteOnline = 1;
    private boolean monthlyReset = true;

    // top
    private int topSize = 10;

    // backup
    private boolean backupEnabled = true;
    private int backupIntervalHours = 24;
    private int backupKeep = 7;

    // settings
    private boolean debug = false;
    private boolean placeholderApi = true;
    private boolean joinReminder = true;
    private boolean joinReminderNoGuild = true;
    private boolean chatShowTag = true;

    // roles
    private final Map<GuildRole, String> roleDisplay = new EnumMap<>(GuildRole.class);

    public PluginConfig(ZHMguildPlugin plugin) {
        this.plugin = plugin;
        for (GuildRole role : GuildRole.values()) {
            roleDisplay.put(role, role.defaultDisplay());
        }
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        storageType = cfg.getString("storage.type", "SQLITE").toUpperCase();
        tablePrefix = cfg.getString("storage.table-prefix", "zhm_");
        autoSaveMinutes = cfg.getInt("storage.auto-save-minutes", 10);
        mysqlHost = cfg.getString("storage.mysql.host", "127.0.0.1");
        mysqlPort = cfg.getInt("storage.mysql.port", 3306);
        mysqlDatabase = cfg.getString("storage.mysql.database", "zhmguild");
        mysqlUsername = cfg.getString("storage.mysql.username", "root");
        mysqlPassword = cfg.getString("storage.mysql.password", "");
        mysqlPoolSize = Math.max(1, cfg.getInt("storage.mysql.pool-size", 6));
        mysqlUseSsl = cfg.getBoolean("storage.mysql.use-ssl", false);
        mysqlParameters = cfg.getString("storage.mysql.parameters", "");
        sqliteFile = cfg.getString("storage.sqlite.file", "zhmguild.db");

        nameMinLength = cfg.getInt("guild.name-min-length", 2);
        nameMaxLength = cfg.getInt("guild.name-max-length", 12);
        allowColorName = cfg.getBoolean("guild.allow-color-name", false);
        nameCaseSensitive = cfg.getBoolean("guild.name-case-sensitive", false);
        maxApplications = cfg.getInt("guild.max-applications", 5);
        applicationExpireHours = cfg.getInt("guild.application-expire-hours", 168);
        defaultMaxMembers = Math.max(1, cfg.getInt("guild.default-max-members", 10));
        createMoney = cfg.getDouble("guild.create.money", 1000.0D);
        createPoints = cfg.getInt("guild.create.points", 0);
        createItems = cfg.getStringList("guild.create.items");
        createCooldown = cfg.getInt("guild.create.cooldown", 60);
        dissolveRefund = cfg.getBoolean("guild.dissolve-refund", false);
        renameCost = cfg.getDouble("guild.rename-cost", 5000.0D);
        contributeMin = cfg.getDouble("guild.contribute.min", 100.0D);
        List<Double> presets = new ArrayList<>();
        for (Object raw : cfg.getList("guild.contribute.presets", List.of(1000.0D, 10000.0D))) {
            if (raw instanceof Number number) {
                presets.add(number.doubleValue());
            }
        }
        if (!presets.isEmpty()) {
            contributePresets = presets;
        }

        levels.clear();
        ConfigurationSection levelSection = cfg.getConfigurationSection("levels");
        if (levelSection != null) {
            for (String key : levelSection.getKeys(false)) {
                if (!key.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                int level = Integer.parseInt(key);
                ConfigurationSection section = levelSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                levels.put(level, new LevelDef(
                        Math.max(1, section.getInt("max-members", defaultMaxMembers)),
                        section.getDouble("up-money", 0.0D),
                        section.getInt("up-active", 0),
                        section.getString("tag", "")
                ));
            }
        }
        maxLevel = cfg.getInt("levels.max-level", levels.isEmpty() ? 1 : levels.keySet().stream().max(Integer::compareTo).orElse(1));

        homeTeleportCooldown = cfg.getInt("home.teleport-cooldown", 30);
        homeTeleportDelay = cfg.getInt("home.teleport-delay", 3);
        homeCancelOnMove = cfg.getBoolean("home.cancel-on-move", true);
        homeSetPermission = GuildRole.parse(cfg.getString("home.set-permission", "VICE"), GuildRole.VICE);

        signInMoney = cfg.getDouble("sign-in.money", 200.0D);
        signInPoints = cfg.getInt("sign-in.points", 0);
        signInContribution = cfg.getDouble("sign-in.contribution", 10.0D);
        signInGuildActive = cfg.getInt("sign-in.guild-active", 20);
        signInGuildMoney = cfg.getDouble("sign-in.guild-money", 0.0D);
        signInResetHours = cfg.getInt("sign-in.reset-hours", 48);
        streakBonus.clear();
        ConfigurationSection streakSection = cfg.getConfigurationSection("sign-in.streak-bonus");
        if (streakSection != null) {
            for (String key : streakSection.getKeys(false)) {
                try {
                    streakBonus.put(Integer.parseInt(key), streakSection.getDouble(key, 1.0D));
                } catch (NumberFormatException ignored) {
                    // 忽略非法配置
                }
            }
        }

        chatFormat = cfg.getString("chat.format", chatFormat);
        chatQuickPrefix = cfg.getString("chat.quick-prefix", "!");
        chatSpyEnabled = cfg.getBoolean("chat.spy-enabled", true);

        activePerMinuteOnline = cfg.getInt("active.per-minute-online", 1);
        monthlyReset = cfg.getBoolean("active.monthly-reset", true);

        topSize = Math.max(1, cfg.getInt("top.size", 10));

        backupEnabled = cfg.getBoolean("backup.enabled", true);
        backupIntervalHours = Math.max(1, cfg.getInt("backup.interval-hours", 24));
        backupKeep = Math.max(1, cfg.getInt("backup.keep", 7));

        debug = cfg.getBoolean("settings.debug", false);
        placeholderApi = cfg.getBoolean("settings.placeholderapi", true);
        joinReminder = cfg.getBoolean("settings.join-reminder", true);
        joinReminderNoGuild = cfg.getBoolean("settings.join-reminder-no-guild", true);
        chatShowTag = cfg.getBoolean("settings.chat-show-tag", true);

        for (GuildRole role : GuildRole.values()) {
            String display = cfg.getString("roles." + role.name());
            roleDisplay.put(role, display == null || display.isEmpty() ? role.defaultDisplay() : display);
        }
    }

    /** 取指定等级配置, 超出配置范围时取最高一级。 */
    public LevelDef levelDef(int level) {
        if (levels.isEmpty()) {
            return new LevelDef(defaultMaxMembers, 0.0D, 0, "");
        }
        LevelDef def = levels.get(level);
        if (def != null) {
            return def;
        }
        return levels.entrySet().stream()
                .filter(entry -> entry.getKey() <= level)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElseGet(() -> levels.entrySet().stream().min(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(
                        new LevelDef(defaultMaxMembers, 0.0D, 0, "")));
    }

    public int maxMembersFor(int level) {
        return levelDef(level).maxMembers();
    }

    public String roleDisplay(GuildRole role) {
        return roleDisplay.getOrDefault(role, role.defaultDisplay());
    }

    public boolean isMysql() {
        return "MYSQL".equalsIgnoreCase(storageType);
    }

    // ---------------- getters ----------------

    public String storageType() {
        return storageType;
    }

    public String tablePrefix() {
        return tablePrefix;
    }

    public int autoSaveMinutes() {
        return autoSaveMinutes;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUsername() {
        return mysqlUsername;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public int mysqlPoolSize() {
        return mysqlPoolSize;
    }

    public boolean mysqlUseSsl() {
        return mysqlUseSsl;
    }

    public String mysqlParameters() {
        return mysqlParameters;
    }

    public String sqliteFile() {
        return sqliteFile;
    }

    public int nameMinLength() {
        return nameMinLength;
    }

    public int nameMaxLength() {
        return nameMaxLength;
    }

    public boolean allowColorName() {
        return allowColorName;
    }

    public boolean nameCaseSensitive() {
        return nameCaseSensitive;
    }

    public int maxApplications() {
        return maxApplications;
    }

    public int applicationExpireHours() {
        return applicationExpireHours;
    }

    public int defaultMaxMembers() {
        return defaultMaxMembers;
    }

    public double createMoney() {
        return createMoney;
    }

    public int createPoints() {
        return createPoints;
    }

    public List<String> createItems() {
        return createItems;
    }

    public int createCooldown() {
        return createCooldown;
    }

    public boolean dissolveRefund() {
        return dissolveRefund;
    }

    public double renameCost() {
        return renameCost;
    }

    public double contributeMin() {
        return contributeMin;
    }

    public List<Double> contributePresets() {
        return contributePresets;
    }

    public Map<Integer, LevelDef> levels() {
        return levels;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int homeTeleportCooldown() {
        return homeTeleportCooldown;
    }

    public int homeTeleportDelay() {
        return homeTeleportDelay;
    }

    public boolean homeCancelOnMove() {
        return homeCancelOnMove;
    }

    public GuildRole homeSetPermission() {
        return homeSetPermission;
    }

    public double signInMoney() {
        return signInMoney;
    }

    public int signInPoints() {
        return signInPoints;
    }

    public double signInContribution() {
        return signInContribution;
    }

    public int signInGuildActive() {
        return signInGuildActive;
    }

    public double signInGuildMoney() {
        return signInGuildMoney;
    }

    public int signInResetHours() {
        return signInResetHours;
    }

    public Map<Integer, Double> streakBonus() {
        return streakBonus;
    }

    public String chatFormat() {
        return chatFormat;
    }

    public String chatQuickPrefix() {
        return chatQuickPrefix;
    }

    public boolean chatSpyEnabled() {
        return chatSpyEnabled;
    }

    public int activePerMinuteOnline() {
        return activePerMinuteOnline;
    }

    public boolean monthlyReset() {
        return monthlyReset;
    }

    public int topSize() {
        return topSize;
    }

    public boolean backupEnabled() {
        return backupEnabled;
    }

    public int backupIntervalHours() {
        return backupIntervalHours;
    }

    public int backupKeep() {
        return backupKeep;
    }

    public boolean debug() {
        return debug;
    }

    public boolean placeholderApi() {
        return placeholderApi;
    }

    public boolean joinReminder() {
        return joinReminder;
    }

    public boolean joinReminderNoGuild() {
        return joinReminderNoGuild;
    }

    public boolean chatShowTag() {
        return chatShowTag;
    }
}
