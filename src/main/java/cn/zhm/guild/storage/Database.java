package cn.zhm.guild.storage;

import cn.zhm.guild.ZHMguildPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * 数据库连接管理 (SQLite / MySQL) 与建表。
 */
public class Database {

    private final ZHMguildPlugin plugin;
    private HikariDataSource dataSource;
    private boolean mysql;
    private String prefix = "zhm_";

    public Database(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws Exception {
        this.mysql = plugin.getConfigManager().isMysql();
        this.prefix = plugin.getConfigManager().tablePrefix();

        HikariConfig config = new HikariConfig();
        config.setPoolName("ZHMguild-Pool");
        config.setConnectionTimeout(15000L);
        config.setMaxLifetime(1800000L);
        config.setKeepaliveTime(60000L);

        if (mysql) {
            String url = "jdbc:mysql://" + plugin.getConfigManager().mysqlHost() + ":"
                    + plugin.getConfigManager().mysqlPort() + "/"
                    + plugin.getConfigManager().mysqlDatabase()
                    + "?useSSL=" + plugin.getConfigManager().mysqlUseSsl()
                    + (plugin.getConfigManager().mysqlParameters().isEmpty() ? "" : "&" + plugin.getConfigManager().mysqlParameters());
            config.setJdbcUrl(url);
            config.setUsername(plugin.getConfigManager().mysqlUsername());
            config.setPassword(plugin.getConfigManager().mysqlPassword());
            config.setMaximumPoolSize(plugin.getConfigManager().mysqlPoolSize());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File file = new File(plugin.getDataFolder(), plugin.getConfigManager().sqliteFile());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录: " + parent.getAbsolutePath());
            }
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            // SQLite 单文件写入, 单连接即可避免锁竞争
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
            config.setAutoCommit(true);
        }

        dataSource = new HikariDataSource(config);
        createTables();
        plugin.getLogger().info("数据库连接成功 (" + (mysql ? "MySQL" : "SQLite") + ")");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("数据库尚未初始化");
        }
        return dataSource.getConnection();
    }

    public boolean isMysql() {
        return mysql;
    }

    public String prefix() {
        return prefix;
    }

    public String guildTable() {
        return prefix + "guild";
    }

    public String memberTable() {
        return prefix + "member";
    }

    public String applicationTable() {
        return prefix + "application";
    }

    private String autoIncrementPk() {
        return mysql
                ? "INT NOT NULL AUTO_INCREMENT PRIMARY KEY"
                : "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    private String tableSuffix() {
        return mysql ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci" : "";
    }

    private void createTables() {
        String guildSql = "CREATE TABLE IF NOT EXISTS " + guildTable() + " ("
                + "id " + autoIncrementPk() + ", "
                + "name VARCHAR(64) NOT NULL, "
                + "name_lower VARCHAR(64) NOT NULL, "
                + "tag VARCHAR(128) DEFAULT '', "
                + "icon VARCHAR(64) DEFAULT 'WHITE_BANNER', "
                + "leader CHAR(36) NOT NULL, "
                + "leader_name VARCHAR(32) DEFAULT '', "
                + "create_time BIGINT NOT NULL DEFAULT 0, "
                + "level INT NOT NULL DEFAULT 1, "
                + "active INT NOT NULL DEFAULT 0, "
                + "month_active INT NOT NULL DEFAULT 0, "
                + "funds DOUBLE NOT NULL DEFAULT 0, "
                + "ore INT NOT NULL DEFAULT 0, "
                + "notice VARCHAR(255) DEFAULT '', "
                + "pvp INT NOT NULL DEFAULT 0, "
                + "home_world VARCHAR(64) DEFAULT NULL, "
                + "home_x DOUBLE DEFAULT 0, "
                + "home_y DOUBLE DEFAULT 0, "
                + "home_z DOUBLE DEFAULT 0, "
                + "home_yaw DOUBLE DEFAULT 0, "
                + "home_pitch DOUBLE DEFAULT 0, "
                + "UNIQUE (name_lower)"
                + ")" + tableSuffix();

        String memberSql = "CREATE TABLE IF NOT EXISTS " + memberTable() + " ("
                + "id " + autoIncrementPk() + ", "
                + "guild_id INT NOT NULL, "
                + "uuid CHAR(36) NOT NULL, "
                + "name VARCHAR(32) DEFAULT '', "
                + "role VARCHAR(16) NOT NULL DEFAULT 'MEMBER', "
                + "contribution DOUBLE NOT NULL DEFAULT 0, "
                + "join_time BIGINT NOT NULL DEFAULT 0, "
                + "last_sign_in BIGINT NOT NULL DEFAULT 0, "
                + "sign_streak INT NOT NULL DEFAULT 0, "
                + "UNIQUE (guild_id, uuid)"
                + ")" + tableSuffix();

        String applicationSql = "CREATE TABLE IF NOT EXISTS " + applicationTable() + " ("
                + "id " + autoIncrementPk() + ", "
                + "guild_id INT NOT NULL, "
                + "uuid CHAR(36) NOT NULL, "
                + "name VARCHAR(32) DEFAULT '', "
                + "type VARCHAR(16) NOT NULL DEFAULT 'APPLY', "
                + "time BIGINT NOT NULL DEFAULT 0, "
                + "UNIQUE (guild_id, uuid)"
                + ")" + tableSuffix();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(guildSql);
            statement.executeUpdate(memberSql);
            statement.executeUpdate(applicationSql);
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "创建数据表失败", exception);
        }
    }
}
