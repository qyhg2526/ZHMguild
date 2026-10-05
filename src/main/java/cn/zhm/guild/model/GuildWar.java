package cn.zhm.guild.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一场进行中的匹配公会战。
 */
public class GuildWar {

    public enum State {
        /** 准备阶段: 已传送到场, 倒计时中, 不可互相攻击。 */
        PREPARING,
        /** 战斗中。 */
        RUNNING,
        /** 已结束, 等待传送回原位。 */
        ENDED
    }

    public enum Side {
        RED, BLUE;

        public Side opponent() {
            return this == RED ? BLUE : RED;
        }
    }

    private final int id;
    private final Arena arena;
    private final Guild redGuild;
    private final Guild blueGuild;

    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Side> sides = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> alive = new ConcurrentHashMap<>();
    private final Map<UUID, Location> returnLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();

    private State state = State.PREPARING;
    /** 当前阶段结束时间戳。 */
    private long phaseEndTime;
    private long battleStartTime;
    private int redKills;
    private int blueKills;
    /** 胜方公会名, null 表示平局。 */
    private String winnerName;

    public GuildWar(int id, Arena arena, Guild redGuild, Guild blueGuild) {
        this.id = id;
        this.arena = arena;
        this.redGuild = redGuild;
        this.blueGuild = blueGuild;
    }

    // ---------------- 基础 ----------------

    public int getId() {
        return id;
    }

    public Arena getArena() {
        return arena;
    }

    public Guild getRedGuild() {
        return redGuild;
    }

    public Guild getBlueGuild() {
        return blueGuild;
    }

    public Guild getGuild(Side side) {
        return side == Side.RED ? redGuild : blueGuild;
    }

    public Guild getOpponent(Guild guild) {
        if (guild == null) {
            return null;
        }
        return guild.getId() == redGuild.getId() ? blueGuild : redGuild;
    }

    public Side getSide(Guild guild) {
        if (guild == null) {
            return null;
        }
        return guild.getId() == redGuild.getId() ? Side.RED : Side.BLUE;
    }

    public State getState() {
        return state;
    }

    public long getPhaseEndTime() {
        return phaseEndTime;
    }

    public void setPhaseEndTime(long phaseEndTime) {
        this.phaseEndTime = phaseEndTime;
    }

    public long getBattleStartTime() {
        return battleStartTime;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    // ---------------- 阶段 ----------------

    public void toRunning() {
        this.state = State.RUNNING;
        this.battleStartTime = System.currentTimeMillis();
    }

    public void toEnded() {
        this.state = State.ENDED;
    }

    /** 当前阶段剩余秒数。 */
    public long remainingSeconds() {
        return Math.max(0L, (phaseEndTime - System.currentTimeMillis()) / 1000L);
    }

    // ---------------- 参与者 ----------------

    public boolean isParticipant(UUID uuid) {
        return uuid != null && participants.contains(uuid);
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public List<UUID> getParticipants(Side side) {
        List<UUID> list = new ArrayList<>();
        for (UUID uuid : participants) {
            if (sides.get(uuid) == side) {
                list.add(uuid);
            }
        }
        return list;
    }

    public void addParticipant(UUID uuid, Side side) {
        participants.add(uuid);
        sides.put(uuid, side);
        alive.put(uuid, Boolean.TRUE);
    }

    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
        sides.remove(uuid);
        alive.remove(uuid);
        returnLocations.remove(uuid);
        kills.remove(uuid);
    }

    public Side getSide(UUID uuid) {
        return sides.get(uuid);
    }

    public boolean isAlive(UUID uuid) {
        return Boolean.TRUE.equals(alive.get(uuid));
    }

    public boolean isEliminated(UUID uuid) {
        return participants.contains(uuid) && !isAlive(uuid);
    }

    public void eliminate(UUID uuid) {
        if (participants.contains(uuid)) {
            alive.put(uuid, Boolean.FALSE);
        }
    }

    public int aliveCount(Side side) {
        int count = 0;
        for (Map.Entry<UUID, Boolean> entry : alive.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) && sides.get(entry.getKey()) == side) {
                count++;
            }
        }
        return count;
    }

    public int participantCount(Side side) {
        int count = 0;
        for (Side value : sides.values()) {
            if (value == side) {
                count++;
            }
        }
        return count;
    }

    public Map<UUID, Location> getReturnLocations() {
        return returnLocations;
    }

    public void setReturnLocation(UUID uuid, Location location) {
        if (location != null) {
            returnLocations.put(uuid, location.clone());
        }
    }

    public Location getReturnLocation(UUID uuid) {
        return returnLocations.get(uuid);
    }

    // ---------------- 击杀 ----------------

    public void addKill(UUID killer) {
        kills.merge(killer, 1, Integer::sum);
        Side side = sides.get(killer);
        if (side == Side.RED) {
            redKills++;
        } else if (side == Side.BLUE) {
            blueKills++;
        }
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getKills(Side side) {
        return side == Side.RED ? redKills : blueKills;
    }

    /** 判定胜负: 返回胜方公会, 平局返回 null。 */
    public Guild judge() {
        int redAlive = aliveCount(Side.RED);
        int blueAlive = aliveCount(Side.BLUE);
        if (redAlive > 0 && blueAlive == 0) {
            return redGuild;
        }
        if (blueAlive > 0 && redAlive == 0) {
            return blueGuild;
        }
        if (redAlive == 0 && blueAlive == 0) {
            return null;
        }
        // 时间到: 先比存活人数, 再比击杀
        if (redAlive != blueAlive) {
            return redAlive > blueAlive ? redGuild : blueGuild;
        }
        if (redKills != blueKills) {
            return redKills > blueKills ? redGuild : blueGuild;
        }
        return null;
    }
}
