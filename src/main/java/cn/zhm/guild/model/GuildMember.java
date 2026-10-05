package cn.zhm.guild.model;

import java.util.UUID;

/**
 * 公会成员数据。
 */
public class GuildMember {

    private final UUID uuid;
    private String name;
    private GuildRole role;
    private double contribution;
    private long joinTime;
    private long lastSignIn;
    private int signStreak;

    public GuildMember(UUID uuid, String name, GuildRole role, double contribution, long joinTime) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
        this.contribution = contribution;
        this.joinTime = joinTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name == null ? "未知" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GuildRole getRole() {
        return role == null ? GuildRole.MEMBER : role;
    }

    public void setRole(GuildRole role) {
        this.role = role;
    }

    public double getContribution() {
        return contribution;
    }

    public void setContribution(double contribution) {
        this.contribution = Math.max(0, contribution);
    }

    public void addContribution(double amount) {
        setContribution(this.contribution + amount);
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public long getLastSignIn() {
        return lastSignIn;
    }

    public void setLastSignIn(long lastSignIn) {
        this.lastSignIn = lastSignIn;
    }

    public int getSignStreak() {
        return signStreak;
    }

    public void setSignStreak(int signStreak) {
        this.signStreak = Math.max(0, signStreak);
    }

    public boolean isLeader() {
        return getRole() == GuildRole.LEADER;
    }

    public boolean canManage() {
        return getRole().atLeast(GuildRole.VICE);
    }
}
