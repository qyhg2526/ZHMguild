package cn.zhm.guild.config;

import java.util.List;

/**
 * 公会战奖励配置。
 *
 * @param contribution 每个参战成员获得的个人贡献
 * @param guildActive  公会获得的活跃
 * @param guildFunds   公会获得的资金
 * @param commands     额外执行的控制台指令, 支持 {player} 与 {guild} 占位符
 */
public record WarReward(double contribution, int guildActive, double guildFunds, List<String> commands) {

    public static WarReward empty() {
        return new WarReward(0.0D, 0, 0.0D, List.of());
    }
}
