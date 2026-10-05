package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.config.WarReward;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 公会战奖励预览。
 */
public class WarRewardMenu extends Gui {

    public WarRewardMenu(ZHMguildPlugin plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    protected Component title() {
        return titleOf("war-reward", "<dark_gray>公会战奖励</dark_gray>", Placeholders.of());
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("war-reward", 27);
    }

    @Override
    protected void build() {
        button("war-reward", "win", GuiUtil.def(10, "GOLDEN_APPLE", "<green>胜利奖励</green>",
                        "<gray>个人贡献: <yellow>{contribution}</yellow></gray>",
                        "<gray>公会活跃: <yellow>{guild_active}</yellow></gray>",
                        "<gray>公会资金: <yellow>{guild_funds}</yellow></gray>"),
                of(plugin.getConfigManager().guildWarWinReward()), null);

        button("war-reward", "lose", GuiUtil.def(12, "BREAD", "<yellow>失败奖励</yellow>",
                        "<gray>个人贡献: <yellow>{contribution}</yellow></gray>",
                        "<gray>公会活跃: <yellow>{guild_active}</yellow></gray>",
                        "<gray>公会资金: <yellow>{guild_funds}</yellow></gray>"),
                of(plugin.getConfigManager().guildWarLoseReward()), null);

        button("war-reward", "draw", GuiUtil.def(14, "COOKIE", "<gray>平局奖励</gray>",
                        "<gray>个人贡献: <yellow>{contribution}</yellow></gray>",
                        "<gray>公会活跃: <yellow>{guild_active}</yellow></gray>",
                        "<gray>公会资金: <yellow>{guild_funds}</yellow></gray>"),
                of(plugin.getConfigManager().guildWarDrawReward()), null);

        button("war-reward", "per-kill", GuiUtil.def(16, "IRON_SWORD", "<red>击杀奖励</red>",
                        "<gray>每击杀一名敌人额外获得:</gray>",
                        "<gray>个人贡献: <yellow>{contribution}</yellow></gray>"),
                of(plugin.getConfigManager().guildWarPerKillReward()), null);

        addBackButton(() -> plugin.getGuiManager().openGuildWar(viewer));
        fillEmpty();
    }

    private Placeholders of(WarReward reward) {
        String commands = reward.commands().isEmpty()
                ? "<gray>无</gray>"
                : String.join("<dark_gray>, </dark_gray>", reward.commands().stream().map(Text::escape).toList());
        return Placeholders.of()
                .put("contribution", TimeUtil.money(reward.contribution()))
                .put("guild_active", reward.guildActive())
                .put("guild_funds", TimeUtil.money(reward.guildFunds()))
                .putRaw("commands", commands);
    }
}
