package cn.zhm.guild.config;

/**
 * 公会等级配置。
 *
 * @param maxMembers 该等级的最大成员数
 * @param upMoney    升级到下一级所需的公会资金
 * @param upActive   升级到下一级所需的公会活跃
 */
public record LevelDef(int maxMembers, double upMoney, int upActive) {
}
