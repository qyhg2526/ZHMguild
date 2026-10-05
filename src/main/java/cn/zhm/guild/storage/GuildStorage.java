package cn.zhm.guild.storage;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.ApplicationType;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 公会数据访问对象。所有方法都应在异步线程调用。
 */
public class GuildStorage {

    private final ZHMguildPlugin plugin;
    private final Database database;

    public GuildStorage(ZHMguildPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    // ---------------------------------------------------------
    // 载入
    // ---------------------------------------------------------

    public List<Guild> loadGuilds() {
        List<Guild> guilds = new ArrayList<>();
        String sql = "SELECT * FROM " + database.guildTable();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Guild guild = new Guild(rs.getInt("id"), rs.getString("name"));
                guild.setIcon(rs.getString("icon"));
                guild.setLeaderName(rs.getString("leader_name"));
                String leader = rs.getString("leader");
                if (leader != null && !leader.isEmpty()) {
                    try {
                        guild.setLeader(UUID.fromString(leader));
                    } catch (IllegalArgumentException ignored) {
                        // 忽略损坏的 UUID
                    }
                }
                guild.setCreateTime(rs.getLong("create_time"));
                guild.setLevel(rs.getInt("level"));
                guild.setActive(rs.getInt("active"));
                guild.setMonthActive(rs.getInt("month_active"));
                guild.setFunds(rs.getDouble("funds"));
                guild.setOre(rs.getInt("ore"));
                guild.setNotice(rs.getString("notice"));
                guild.setPvp(rs.getInt("pvp") == 1);

                String worldName = rs.getString("home_world");
                if (worldName != null && !worldName.isEmpty()) {
                    World world = Bukkit.getWorld(worldName);
                    Location location = new Location(world,
                            rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                            (float) rs.getDouble("home_yaw"), (float) rs.getDouble("home_pitch"));
                    guild.setHome(location);
                }
                guilds.add(guild);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "载入公会数据失败", exception);
        }
        return guilds;
    }

    public Map<Integer, List<GuildMember>> loadMembers() {
        Map<Integer, List<GuildMember>> result = new HashMap<>();
        String sql = "SELECT * FROM " + database.memberTable();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int guildId = rs.getInt("guild_id");
                UUID uuid;
                try {
                    uuid = UUID.fromString(rs.getString("uuid"));
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                GuildMember member = new GuildMember(
                        uuid,
                        rs.getString("name"),
                        GuildRole.parse(rs.getString("role"), GuildRole.MEMBER),
                        rs.getDouble("contribution"),
                        rs.getLong("join_time")
                );
                member.setLastSignIn(rs.getLong("last_sign_in"));
                member.setSignStreak(rs.getInt("sign_streak"));
                result.computeIfAbsent(guildId, key -> new ArrayList<>()).add(member);
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "载入公会成员失败", exception);
        }
        return result;
    }

    public List<GuildApplication> loadApplications() {
        List<GuildApplication> result = new ArrayList<>();
        String sql = "SELECT * FROM " + database.applicationTable();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rs.getString("uuid"));
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                result.add(new GuildApplication(
                        rs.getInt("guild_id"),
                        uuid,
                        rs.getString("name"),
                        ApplicationType.parse(rs.getString("type")),
                        rs.getLong("time")
                ));
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "载入入会申请失败", exception);
        }
        return result;
    }

    // ---------------------------------------------------------
    // 公会
    // ---------------------------------------------------------

    /**
     * 插入公会。ID 由 GuildManager 在内存中分配, 这里显式写入。
     * 返回是否成功。
     */
    public boolean insertGuild(Guild guild) {
        String sql = "INSERT INTO " + database.guildTable()
                + " (id, name, name_lower, icon, leader, leader_name, create_time, level, active, month_active, funds, ore, notice, pvp,"
                + " home_world, home_x, home_y, home_z, home_yaw, home_pitch)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guild.getId());
            statement.setString(2, guild.getName());
            statement.setString(3, guild.getNameLower());
            statement.setString(4, guild.getIcon());
            statement.setString(5, guild.getLeader() == null ? "" : guild.getLeader().toString());
            statement.setString(6, guild.getLeaderName());
            statement.setLong(7, guild.getCreateTime());
            statement.setInt(8, guild.getLevel());
            statement.setInt(9, guild.getActive());
            statement.setInt(10, guild.getMonthActive());
            statement.setDouble(11, guild.getFunds());
            statement.setInt(12, guild.getOre());
            statement.setString(13, guild.getNotice());
            statement.setInt(14, guild.isPvp() ? 1 : 0);
            Location home = guild.getHome();
            if (home != null && home.getWorld() != null) {
                statement.setString(15, home.getWorld().getName());
                statement.setDouble(16, home.getX());
                statement.setDouble(17, home.getY());
                statement.setDouble(18, home.getZ());
                statement.setDouble(19, home.getYaw());
                statement.setDouble(20, home.getPitch());
            } else {
                statement.setString(15, null);
                statement.setDouble(16, 0);
                statement.setDouble(17, 0);
                statement.setDouble(18, 0);
                statement.setDouble(19, 0);
                statement.setDouble(20, 0);
            }
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "创建公会记录失败: " + guild.getName(), exception);
            return false;
        }
    }

    public void updateGuild(Guild guild) {
        String sql = "UPDATE " + database.guildTable()
                + " SET name=?, name_lower=?, icon=?, leader=?, leader_name=?, level=?, active=?, month_active=?,"
                + " funds=?, ore=?, notice=?, pvp=?, home_world=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?"
                + " WHERE id=?";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getName());
            statement.setString(2, guild.getNameLower());
            statement.setString(3, guild.getIcon());
            statement.setString(4, guild.getLeader() == null ? "" : guild.getLeader().toString());
            statement.setString(5, guild.getLeaderName());
            statement.setInt(6, guild.getLevel());
            statement.setInt(7, guild.getActive());
            statement.setInt(8, guild.getMonthActive());
            statement.setDouble(9, guild.getFunds());
            statement.setInt(10, guild.getOre());
            statement.setString(11, guild.getNotice());
            statement.setInt(12, guild.isPvp() ? 1 : 0);
            Location home = guild.getHome();
            if (home != null && home.getWorld() != null) {
                statement.setString(13, home.getWorld().getName());
                statement.setDouble(14, home.getX());
                statement.setDouble(15, home.getY());
                statement.setDouble(16, home.getZ());
                statement.setDouble(17, home.getYaw());
                statement.setDouble(18, home.getPitch());
            } else {
                statement.setString(13, null);
                statement.setDouble(14, 0);
                statement.setDouble(15, 0);
                statement.setDouble(16, 0);
                statement.setDouble(17, 0);
                statement.setDouble(18, 0);
            }
            statement.setInt(19, guild.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "更新公会记录失败: " + guild.getName(), exception);
        }
    }

    public void deleteGuild(int guildId) {
        try (Connection connection = database.connection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + database.memberTable() + " WHERE guild_id=?")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + database.applicationTable() + " WHERE guild_id=?")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + database.guildTable() + " WHERE id=?")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "删除公会记录失败: " + guildId, exception);
        }
    }

    // ---------------------------------------------------------
    // 成员
    // ---------------------------------------------------------

    public void upsertMember(int guildId, GuildMember member) {
        String sql = database.isMysql()
                ? "INSERT INTO " + database.memberTable()
                + " (guild_id, uuid, name, role, contribution, join_time, last_sign_in, sign_streak)"
                + " VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name), role=VALUES(role),"
                + " contribution=VALUES(contribution), last_sign_in=VALUES(last_sign_in), sign_streak=VALUES(sign_streak)"
                : "INSERT INTO " + database.memberTable()
                + " (guild_id, uuid, name, role, contribution, join_time, last_sign_in, sign_streak)"
                + " VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(guild_id, uuid) DO UPDATE SET name=excluded.name,"
                + " role=excluded.role, contribution=excluded.contribution, last_sign_in=excluded.last_sign_in,"
                + " sign_streak=excluded.sign_streak";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guildId);
            statement.setString(2, member.getUuid().toString());
            statement.setString(3, member.getName());
            statement.setString(4, member.getRole().name());
            statement.setDouble(5, member.getContribution());
            statement.setLong(6, member.getJoinTime());
            statement.setLong(7, member.getLastSignIn());
            statement.setInt(8, member.getSignStreak());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "保存成员数据失败: " + member.getName(), exception);
        }
    }

    public void deleteMember(int guildId, UUID uuid) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + database.memberTable() + " WHERE guild_id=? AND uuid=?")) {
            statement.setInt(1, guildId);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "删除成员数据失败: " + uuid, exception);
        }
    }

    // ---------------------------------------------------------
    // 申请
    // ---------------------------------------------------------

    public void upsertApplication(GuildApplication application) {
        String sql = database.isMysql()
                ? "INSERT INTO " + database.applicationTable()
                + " (guild_id, uuid, name, type, time) VALUES (?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), time=VALUES(time)"
                : "INSERT INTO " + database.applicationTable()
                + " (guild_id, uuid, name, type, time) VALUES (?,?,?,?,?)"
                + " ON CONFLICT(guild_id, uuid) DO UPDATE SET name=excluded.name, type=excluded.type, time=excluded.time";
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, application.getGuildId());
            statement.setString(2, application.getUuid().toString());
            statement.setString(3, application.getName());
            statement.setString(4, application.getType().name());
            statement.setLong(5, application.getTime());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "保存入会申请失败: " + application.getName(), exception);
        }
    }

    public void deleteApplication(int guildId, UUID uuid) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + database.applicationTable() + " WHERE guild_id=? AND uuid=?")) {
            statement.setInt(1, guildId);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "删除入会申请失败: " + uuid, exception);
        }
    }

    public int clearApplications() {
        String sql = "DELETE FROM " + database.applicationTable();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "清理入会申请失败", exception);
            return 0;
        }
    }

    // ---------------------------------------------------------
    // 批量保存
    // ---------------------------------------------------------

    public void saveAll(List<Guild> guilds) {
        for (Guild guild : guilds) {
            updateGuild(guild);
            for (GuildMember member : new ArrayList<>(guild.getMembers())) {
                upsertMember(guild.getId(), member);
            }
            for (GuildApplication application : new ArrayList<>(guild.getApplications())) {
                upsertApplication(application);
            }
        }
    }
}
