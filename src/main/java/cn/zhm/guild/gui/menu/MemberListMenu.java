package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * 公会成员列表。
 */
public class MemberListMenu extends Gui {

    private static final int PER_PAGE = GuiUtil.CONTENT_SLOTS.length;

    private final int page;

    public MemberListMenu(ZHMguildPlugin plugin, Player viewer, int page) {
        super(plugin, viewer);
        this.page = Math.max(1, page);
    }

    @Override
    protected Component title() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        return titleOf("members", "<dark_gray>成员列表</dark_gray> <gray>»</gray> <yellow>{guild}</yellow>",
                Placeholders.of().put("guild", guild == null ? "" : guild.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("members", 54);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        if (guild == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }
        GuildMember self = plugin.getGuildManager().getMember(viewer.getUniqueId());
        List<GuildMember> members = guild.getSortedMembers();
        int pages = GuiUtil.pages(members.size(), PER_PAGE);
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;

        for (int index = 0; index < PER_PAGE && start + index < members.size(); index++) {
            GuildMember member = members.get(start + index);
            boolean online = Bukkit.getPlayer(member.getUuid()) != null;
            Placeholders placeholders = Placeholders.of()
                    .put("player", member.getName())
                    .putRaw("role", plugin.getConfigManager().roleDisplay(member.getRole()))
                    .put("contribution", TimeUtil.money(member.getContribution()))
                    .put("join_time", TimeUtil.formatDate(member.getJoinTime()))
                    .put("last_signin", member.getLastSignIn() <= 0 ? "从未签到" : TimeUtil.formatDate(member.getLastSignIn()))
                    .putRaw("online", online ? "<green>在线</green>" : "<gray>离线</gray>")
                    .put("guild", guild.getName());

            ItemStack item = plugin.getGuiConfig()
                    .template("members.member-item", GuiUtil.def("PLAYER_HEAD", "<yellow>{player}</yellow>",
                            "<gray>职位: {role}</gray>",
                            "<gray>贡献: <yellow>{contribution}</yellow></gray>",
                            "", "<yellow>» 点击管理该成员</yellow>"))
                    .build(placeholders);
            item = applyHead(item, member.getUuid());

            setItem(GuiUtil.CONTENT_SLOTS[index], item, event -> {
                boolean canManage = self != null && self.canManage() && !member.getUuid().equals(viewer.getUniqueId());
                if (canManage) {
                    plugin.getGuiManager().openMemberManage(viewer, member.getUuid(), current);
                } else {
                    plugin.getMessages().send(viewer, "gui.info-title", Placeholders.of()
                            .put("guild", guild.getName()));
                }
            });
        }

        addPageButtons(current, pages, newPage -> plugin.getGuiManager().openMembers(viewer, newPage));
        addBackButton(() -> plugin.getGuiManager().openMain(viewer));
        fillEmpty();
    }

    /** 判断职位是否可管理。 */
    public static boolean canManage(GuildRole self, GuildRole target) {
        return self.atLeast(GuildRole.VICE) && self.above(target);
    }
}
