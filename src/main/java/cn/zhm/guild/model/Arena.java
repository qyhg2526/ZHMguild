package cn.zhm.guild.model;

import org.bukkit.Location;

/**
 * 公会战场地。前两个出生点分别对应红蓝双方, 观看点可选。
 */
public class Arena {

    private final String name;
    private Location redSpawn;
    private Location blueSpawn;
    private Location spectateSpawn;
    /** 是否已被某场公会战占用。 */
    private boolean inUse;
    /** 占用该场地的公会战 id, 未占用为 -1。 */
    private int occupiedBy = -1;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Location getRedSpawn() {
        return redSpawn;
    }

    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
    }

    public Location getBlueSpawn() {
        return blueSpawn;
    }

    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
    }

    public Location getSpectateSpawn() {
        return spectateSpawn;
    }

    public void setSpectateSpawn(Location spectateSpawn) {
        this.spectateSpawn = spectateSpawn;
    }

    public boolean hasSpectate() {
        return spectateSpawn != null && spectateSpawn.getWorld() != null;
    }

    /** 场地是否可用于开战(双方出生点齐全且世界存在)。 */
    public boolean isReady() {
        return redSpawn != null && redSpawn.getWorld() != null
                && blueSpawn != null && blueSpawn.getWorld() != null;
    }

    public boolean isAvailable() {
        return !inUse && isReady();
    }

    public boolean isInUse() {
        return inUse;
    }

    public int getOccupiedBy() {
        return occupiedBy;
    }

    public void occupy(int warId) {
        this.inUse = true;
        this.occupiedBy = warId;
    }

    public void release() {
        this.inUse = false;
        this.occupiedBy = -1;
    }

    /** 用于状态显示的简短描述。 */
    public String describe() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(isReady() ? " (就绪)" : " (缺少出生点)");
        if (hasSpectate()) {
            sb.append(" +观看点");
        }
        return sb.toString();
    }
}
