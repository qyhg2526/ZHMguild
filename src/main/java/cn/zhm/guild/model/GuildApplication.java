package cn.zhm.guild.model;

import java.util.UUID;

/**
 * 入会申请 / 邀请。
 */
public class GuildApplication {

    private final int guildId;
    private final UUID uuid;
    private String name;
    private ApplicationType type;
    private long time;

    public GuildApplication(int guildId, UUID uuid, String name, ApplicationType type, long time) {
        this.guildId = guildId;
        this.uuid = uuid;
        this.name = name;
        this.type = type;
        this.time = time;
    }

    public int getGuildId() {
        return guildId;
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

    public ApplicationType getType() {
        return type;
    }

    public void setType(ApplicationType type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
