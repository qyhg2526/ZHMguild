package cn.zhm.guild;

import cn.zhm.guild.command.GuildCommand;
import cn.zhm.guild.config.GuiConfig;
import cn.zhm.guild.config.Messages;
import cn.zhm.guild.config.PluginConfig;
import cn.zhm.guild.gui.GuiListener;
import cn.zhm.guild.gui.GuiManager;
import cn.zhm.guild.hook.EconomyHook;
import cn.zhm.guild.hook.PlaceholderHook;
import cn.zhm.guild.hook.PointsHook;
import cn.zhm.guild.listener.ChatListener;
import cn.zhm.guild.listener.PlayerListener;
import cn.zhm.guild.manager.BackupManager;
import cn.zhm.guild.manager.ChatInputManager;
import cn.zhm.guild.manager.ConfirmManager;
import cn.zhm.guild.manager.GuildActions;
import cn.zhm.guild.manager.GuildChatManager;
import cn.zhm.guild.manager.GuildManager;
import cn.zhm.guild.manager.TeleportManager;
import cn.zhm.guild.storage.Database;
import cn.zhm.guild.storage.GuildStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDate;
import java.util.logging.Level;

/**
 * ZHMguild 主类。
 */
public class ZHMguildPlugin extends JavaPlugin {

    private PluginConfig configManager;
    private Messages messages;
    private GuiConfig guiConfig;

    private Database database;
    private GuildStorage storage;
    private GuildManager guildManager;
    private GuildActions guildActions;
    private GuildChatManager guildChatManager;
    private GuiManager guiManager;

    private EconomyHook economyHook;
    private PointsHook pointsHook;
    private PlaceholderHook placeholderHook;

    private ChatInputManager chatInputManager;
    private ConfirmManager confirmManager;
    private TeleportManager teleportManager;
    private BackupManager backupManager;

    private File dataFile;
    private YamlConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new PluginConfig(this);
        configManager.load();

        messages = new Messages(this);
        messages.load();

        guiConfig = new GuiConfig(this);
        guiConfig.load();

        database = new Database(this);
        try {
            database.connect();
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "数据库连接失败, ZHMguild 将被禁用。请检查 config.yml 中的存储配置。", throwable);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storage = new GuildStorage(this, database);
        guildManager = new GuildManager(this);
        guildManager.loadAll();
        guildActions = new GuildActions(this);
        guildChatManager = new GuildChatManager(this);
        guiManager = new GuiManager(this);

        economyHook = new EconomyHook(this);
        economyHook.setup();
        pointsHook = new PointsHook(this);
        pointsHook.setup();

        chatInputManager = new ChatInputManager(this);
        confirmManager = new ConfirmManager(this);
        teleportManager = new TeleportManager(this);
        backupManager = new BackupManager(this);

        GuildCommand executor = new GuildCommand(this);
        PluginCommand command = getCommand("zhmguild");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("无法注册命令 /zhmguild, 请检查 plugin.yml。");
        }

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        loadDataFile();
        setupPlaceholderApi();
        startTasks();

        getLogger().info("ZHMguild 启用完成! 存储类型: " + (database.isMysql() ? "MySQL" : "SQLite")
                + ", 经济: " + (economyHook.isEnabled() ? "Vault" : "未启用")
                + ", 点券: " + (pointsHook.isEnabled() ? "PlayerPoints" : "未启用")
                + ", 变量: " + (placeholderHook != null ? "PlaceholderAPI" : "未启用"));
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
        if (guildManager != null) {
            guildManager.saveAllBlocking();
            guildManager.shutdown();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("ZHMguild 已卸载, 数据已保存。");
    }

    // ---------------------------------------------------------
    // 初始化辅助
    // ---------------------------------------------------------

    private void setupPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            placeholderHook = new PlaceholderHook(this);
            if (configManager.placeholderApi() && placeholderHook.register()) {
                getLogger().info("已注册 PlaceholderAPI 变量: %zhmguild_xxx%");
            }
        } catch (Throwable throwable) {
            getLogger().warning("注册 PlaceholderAPI 变量失败: " + throwable.getMessage());
            placeholderHook = null;
        }
    }

    private void loadDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    getLogger().info("已创建 data.yml");
                }
            } catch (Exception exception) {
                getLogger().warning("创建 data.yml 失败: " + exception.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (Exception exception) {
            getLogger().warning("保存 data.yml 失败: " + exception.getMessage());
        }
    }

    private void startTasks() {
        int autoSave = configManager.autoSaveMinutes();
        if (autoSave > 0) {
            long ticks = autoSave * 60L * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> {
                guildManager.submit(guildManager::saveAllBlocking);
                if (configManager.debug()) {
                    getLogger().info("已自动保存全部公会数据。");
                }
            }, ticks, ticks);
        }

        // 在线活跃: 每分钟
        getServer().getScheduler().runTaskTimer(this, () -> guildManager.tickOnlineActive(), 1200L, 1200L);

        // 过期申请清理: 每 10 分钟
        getServer().getScheduler().runTaskTimer(this, () -> {
            int removed = guildManager.purgeExpiredApplications();
            if (removed > 0 && configManager.debug()) {
                getLogger().info("已清理 " + removed + " 条过期入会申请。");
            }
        }, 1200L, 12000L);

        // 月度活跃重置检查: 每小时
        getServer().getScheduler().runTaskTimer(this, this::checkMonthlyReset, 600L, 72000L);

        // 自动备份
        if (configManager.backupEnabled()) {
            long ticks = configManager.backupIntervalHours() * 3600L * 20L;
            getServer().getScheduler().runTaskTimer(this,
                    () -> guildManager.submit(() -> backupManager.backup()), ticks, ticks);
        }
    }

    private void checkMonthlyReset() {
        if (!configManager.monthlyReset()) {
            return;
        }
        LocalDate now = LocalDate.now();
        String current = now.getYear() + "-" + now.getMonthValue();
        String stored = dataConfig.getString("last-month-reset", "");
        if (!current.equals(stored)) {
            dataConfig.set("last-month-reset", current);
            saveDataFile();
            guildManager.resetMonthActive();
        }
    }

    /** 重载全部配置。 */
    public void reloadAll() {
        configManager.load();
        messages.load();
        guiConfig.load();
    }

    // ---------------------------------------------------------
    // Getters
    // ---------------------------------------------------------

    public PluginConfig getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public Database getDatabase() {
        return database;
    }

    public GuildStorage getStorage() {
        return storage;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public GuildActions getGuildActions() {
        return guildActions;
    }

    public GuildChatManager getGuildChatManager() {
        return guildChatManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public PointsHook getPointsHook() {
        return pointsHook;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public ConfirmManager getConfirmManager() {
        return confirmManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }
}
