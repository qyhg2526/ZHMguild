package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * 单个成员的管理界面。
 */
public class MemberManageMenu extends Gui {

    private final UUID target;
    private final int returnPage;

    public MemberManageMenu(ZHMguildPlugin plugin, Player viewer, UUID target, int returnPage) {
        super(plugin, viewer);
        this.target = target;
        this.returnPage = returnPage;
    }

    @Override
    protected Component title() {
        GuildMember member = plugin.getGuildManager().getMember(target);
        return titleOf("member-manage", "<dark_gray>成员管理</dark_gray> <gray>»</gray> <yellow>{player}</yellow>",
                Placeholders.of().put("player", member == null ? "?" : member.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("member-manage", 27);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember self = plugin.getGuildManager().getMember(viewer.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(target);
        if (guild == null || self == null || member == null || !guild.isMember(target)) {
            plugin.getGuiManager().openMembers(viewer, returnPage);
            return;
        }

        Placeholders placeholders = Placeholders.of()
                .put("player", member.getName())
                .putRaw("role", plugin.getConfigManager().roleDisplay(member.getRole()))
                .put("contribution", TimeUtil.money(member.getContribution()))
                .putRaw("next_role", member.getRole().promote() == null ? "无"
                        : plugin.getConfigManager().roleDisplay(member.getRole().promote()));

        ItemStack head = plugin.getGuiConfig()
                .itemOr("member-manage", "info", GuiUtil.def(4, "PLAYER_HEAD", "<yellow>{player}</yellow>",
                        "<gray>职位: {role}</gray>",
                        "<gray>贡献: <yellow>{contribution}</yellow></gray>"))
                .build(placeholders);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));
            head.setItemMeta(skullMeta);
        }
        setItem(4, head, null);

        button("member-manage", "promote", GuiUtil.def(11, "LIME_DYE", "<green>提升职位</green>",
                        "<gray>当前: {role}</gray>",
                        "<gray>下一级: <yellow>{next_role}</yellow></gray>",
                        "", "<yellow>» 点击提升</yellow>"),
                placeholders, event -> {
                    plugin.getGuildActions().promote(viewer, guild, member);
                    refresh();
                });

        button("member-manage", "demote", GuiUtil.def(13, "GRAY_DYE", "<yellow>降低职位</yellow>",
                        "<gray>当前: {role}</gray>",
                        "", "<yellow>» 点击降低</yellow>"),
                placeholders, event -> {
                    plugin.getGuildActions().demote(viewer, guild, member);
                    refresh();
                });

        if (self.isLeader()) {
            button("member-manage", "transfer", GuiUtil.def(14, "GOLDEN_HELMET", "<gold>转让会长</gold>",
                            "<gray>将该成员设为公会会长</gray>",
                            "<red>此操作不可逆!</red>",
                            "", "<yellow>» 点击转让</yellow>"),
                    placeholders, event -> plugin.getGuiManager().openConfirm(viewer,
                            plugin.getMessages().str("role.transfer-confirm", Placeholders.of()
                                    .put("player", member.getName())),
                            () -> {
                                plugin.getGuildActions().transfer(viewer, guild, member);
                                plugin.getGuiManager().openMain(viewer);
                            }));
        }

        button("member-manage", "kick", GuiUtil.def(15, "IRON_SWORD", "<red>踢出公会</red>",
                        "<gray>将该成员踢出公会</gray>",
                        "", "<yellow>» 点击踢出</yellow>"),
                placeholders, event -> plugin.getGuiManager().openConfirm(viewer,
                        plugin.getMessages().str("kick.confirm-gui", Placeholders.of()
                                .put("player", member.getName())),
                        () -> {
                            plugin.getGuildActions().kick(viewer, guild, member);
                            plugin.getGuiManager().openMembers(viewer, returnPage);
                        }));

        addBackButton(() -> plugin.getGuiManager().openMembers(viewer, returnPage));
        fillEmpty();
    }
}
