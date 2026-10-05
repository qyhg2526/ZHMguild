package cn.zhm.guild.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公会数据。
 */
public class Guild {

    private int id;
    private String name;
    private String icon;
    private UUID leader;
    private String leaderName;
    private long createTime;
    private int level = 1;
    private int active;
    private int monthActive;
    private double funds;
    private int ore;
    private String notice = "";
    private boolean pvp;
    private Location home;

    private final Map<UUID, GuildMember> members = new ConcurrentHashMap<>();
    private final Map<UUID, GuildApplication> applications = new ConcurrentHashMap<>();

    public Guild(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // ---------------- 基础字段 ----------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameLower() {
        return name == null ? "" : name.toLowerCase();
    }

    public String getIcon() {
        return icon == null || icon.isEmpty() ? "WHITE_BANNER" : icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public String getLeaderName() {
        return leaderName == null ? "未知" : leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = Math.max(0, active);
    }

    public void addActive(int amount) {
        setActive(this.active + amount);
    }

    public int getMonthActive() {
        return monthActive;
    }

    public void setMonthActive(int monthActive) {
        this.monthActive = Math.max(0, monthActive);
    }

    public void addMonthActive(int amount) {
        setMonthActive(this.monthActive + amount);
    }

    public double getFunds() {
        return funds;
    }

    public void setFunds(double funds) {
        this.funds = Math.max(0, funds);
    }

    public void addFunds(double amount) {
        setFunds(this.funds + amount);
    }

    public int getOre() {
        return ore;
    }

    public void setOre(int ore) {
        this.ore = Math.max(0, ore);
    }

    public String getNotice() {
        return notice == null ? "" : notice;
    }

    public void setNotice(String notice) {
        this.notice = notice == null ? "" : notice;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public boolean hasHome() {
        if (home == null) {
            return false;
        }
        World world = home.getWorld();
        return world != null;
    }

    /** 若主城所在世界已被删除, 返回 null。 */
    public Location getSafeHome() {
        if (home == null) {
            return null;
        }
        World world = Bukkit.getWorld(home.getWorld() == null ? "" : home.getWorld().getName());
        if (world == null) {
            return null;
        }
        Location result = home.clone();
        result.setWorld(world);
        return result;
    }

    // ---------------- 成员 ----------------

    public Map<UUID, GuildMember> getMemberMap() {
        return members;
    }

    public Collection<GuildMember> getMembers() {
        return members.values();
    }

    /** 按职位 + 贡献排序的成员列表。 */
    public List<GuildMember> getSortedMembers() {
        List<GuildMember> list = new ArrayList<>(members.values());
        list.sort(Comparator.comparingInt((GuildMember m) -> -m.getRole().weight())
                .thenComparingDouble(m -> -m.getContribution()));
        return list;
    }

    public int getMemberCount() {
        return members.size();
    }

    public GuildMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void addMember(GuildMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    /** 重新计算会长(用于修复数据)。 */
    public GuildMember findLeaderMember() {
        for (GuildMember member : members.values()) {
            if (member.getRole() == GuildRole.LEADER) {
                return member;
            }
        }
        return null;
    }

    // ---------------- 申请 ----------------

    public Map<UUID, GuildApplication> getApplicationMap() {
        return applications;
    }

    public List<GuildApplication> getApplications() {
        List<GuildApplication> list = new ArrayList<>(applications.values());
        list.sort(Comparator.comparingLong(GuildApplication::getTime).reversed());
        return list;
    }

    public int getApplicationCount() {
        return applications.size();
    }

    public GuildApplication getApplication(UUID uuid) {
        return applications.get(uuid);
    }

    public void addApplication(GuildApplication application) {
        applications.put(application.getUuid(), application);
    }

    public void removeApplication(UUID uuid) {
        applications.remove(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Guild other && other.id == this.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
