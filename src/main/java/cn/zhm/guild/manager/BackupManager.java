package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;

/**
 * 数据备份: SQLite 直接复制文件, MySQL 导出 INSERT 语句。
 */
public class BackupManager {

    private final ZHMguildPlugin plugin;

    public BackupManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public File backupDirectory() {
        File directory = new File(plugin.getDataFolder(), "backUp");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("无法创建备份目录: " + directory.getAbsolutePath());
        }
        return directory;
    }

    /** 执行一次备份。 */
    public void backup() {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        try {
            if (plugin.getDatabase().isMysql()) {
                File target = new File(backupDirectory(), "mysql-" + stamp + ".sql");
                dumpMysql(target);
            } else {
                File source = new File(plugin.getDataFolder(), plugin.getConfigManager().sqliteFile());
                if (!source.exists()) {
                    return;
                }
                File target = new File(backupDirectory(), "sqlite-" + stamp + ".db");
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            cleanOldBackups();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "备份数据失败", throwable);
        }
    }

    private void dumpMysql(File target) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(target, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("-- ZHMguild backup " + new Date());
            for (String table : new String[]{
                    plugin.getDatabase().guildTable(),
                    plugin.getDatabase().memberTable(),
                    plugin.getDatabase().applicationTable()}) {
                dumpTable(writer, table);
            }
        }
    }

    private void dumpTable(PrintWriter writer, String table) throws Exception {
        try (Connection connection = plugin.getDatabase().connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
             ResultSet rs = statement.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            while (rs.next()) {
                StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" VALUES (");
                for (int index = 1; index <= columns; index++) {
                    Object value = rs.getObject(index);
                    if (index > 1) {
                        sql.append(',');
                    }
                    if (value == null) {
                        sql.append("NULL");
                    } else if (value instanceof Number) {
                        sql.append(value);
                    } else {
                        sql.append('\'').append(String.valueOf(value).replace("'", "''")).append('\'');
                    }
                }
                sql.append(");");
                writer.println(sql);
            }
            writer.flush();
        }
    }

    private void cleanOldBackups() {
        File directory = backupDirectory();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".db") || name.endsWith(".sql"));
        if (files == null) {
            return;
        }
        int keep = plugin.getConfigManager().backupKeep();
        if (files.length <= keep) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int index = 0; index < files.length - keep; index++) {
            if (!files[index].delete()) {
                plugin.getLogger().warning("无法删除旧备份: " + files[index].getName());
            }
        }
    }
}
