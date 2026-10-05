package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.ApplicationType;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 公会列表 / 排行浏览界面。
 */
public class GuildListMenu extends Gui {

    private static final int PER_PAGE = GuiUtil.CONTENT_SLOTS.length;

    private final SortType sort;
    private final int page;

    public GuildListMenu(ZHMguildPlugin plugin, Player viewer, SortType sort, int page) {
        super(plugin, viewer);
        this.sort = sort;
        this.page = Math.max(1, page);
    }

    @Override
    protected Component title() {
        return titleOf("guild-list", "<dark_gray>公会列表</dark_gray> <gray>»</gray> <yellow>{sort}</yellow>",
                Placeholders.of().put("sort", sort.display()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("guild-list", 54);
    }

    @Override
    protected void build() {
        List<Guild> guilds = plugin.getGuildManager().sortedGuilds(sort);
        int pages = GuiUtil.pages(guilds.size(), PER_PAGE);
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;

        Placeholders header = Placeholders.of()
                .put("sort", sort.display())
                .put("total", guilds.size())
                .put("page", current)
                .put("pages", pages);
        button("guild-list", "info", GuiUtil.def(4, "COMPASS", "<aqua>排序方式: <yellow>{sort}</yellow></aqua>",
                        "<gray>点击切换排序方式</gray>",
                        "<gray>当前共 <yellow>{total}</yellow> 个公会</gray>"),
                header, event -> plugin.getGuiManager().openGuildList(viewer, sort.next(), 1));

        for (int index = 0; index < PER_PAGE && start + index < guilds.size(); index++) {
            Guild guild = guilds.get(start + index);
            GuildApplication application = guild.getApplication(viewer.getUniqueId());
            String applyStatus = application == null ? "<gray>未申请</gray>"
                    : application.getType() == ApplicationType.INVITE ? "<green>已邀请你</green>" : "<yellow>已申请</yellow>";

            Placeholders placeholders = Placeholders.of()
                    .put("guild", guild.getName())
                    .put("leader", guild.getLeaderName())
                    .put("level", guild.getLevel())
                    .put("members", guild.getMemberCount())
                    .put("max_members", plugin.getGuildManager().maxMembers(guild))
                    .put("active", guild.getActive())
                    .put("funds", TimeUtil.money(guild.getFunds()))
                    .put("rank", start + index + 1)
                    .putRaw("apply_status", applyStatus);

            ItemStack item = plugin.getGuiConfig()
                    .template("guild-list.guild-item", GuiUtil.def("WHITE_BANNER", "<yellow>{guild}</yellow>",
                            "<gray>会长: <yellow>{leader}</yellow></gray>",
                            "<gray>成员: <yellow>{members}</yellow></gray>",
                            "<gray>等级: <yellow>{level}</yellow></gray>",
                            "", "<yellow>» 左键申请加入</yellow>"))
                    .build(placeholders);

            int slot = GuiUtil.CONTENT_SLOTS[index];
            setItem(slot, item, event -> {
                if (application != null && application.getType() == ApplicationType.INVITE) {
                    plugin.getGuildActions().acceptInvite(viewer, guild);
                } else {
                    plugin.getGuildActions().apply(viewer, guild);
                }
                refresh();
            });
        }

        addPageButtons(current, pages, newPage -> plugin.getGuiManager().openGuildList(viewer, sort, newPage));
        addCloseButton();
        fillEmpty();
    }
}
