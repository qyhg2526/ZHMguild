package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 公会排行榜。
 */
public class TopMenu extends Gui {

    private static final int PER_PAGE = GuiUtil.CONTENT_SLOTS.length;

    private final SortType sort;
    private final int page;

    public TopMenu(ZHMguildPlugin plugin, Player viewer, SortType sort, int page) {
        super(plugin, viewer);
        this.sort = sort;
        this.page = Math.max(1, page);
    }

    @Override
    protected Component title() {
        return titleOf("top", "<dark_gray>公会排行</dark_gray> <gray>»</gray> <yellow>{sort}</yellow>",
                Placeholders.of().put("sort", sort.display()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("top", 54);
    }

    @Override
    protected void build() {
        List<Guild> guilds = plugin.getGuildManager().sortedGuilds(sort);
        int pages = GuiUtil.pages(guilds.size(), PER_PAGE);
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;

        // 排序切换按钮
        List<String> sorts = plugin.getGuiConfig().stringList("top.sorts",
                List.of("LEVEL", "ACTIVE", "FUNDS", "MEMBERS"));
        int slot = 0;
        for (String name : sorts) {
            SortType type = SortType.parse(name, null);
            if (type == null) {
                continue;
            }
            boolean active = type == sort;
            ItemStack item = GuiUtil.def(slot, active ? "LIME_DYE" : "GRAY_DYE",
                    (active ? "<green>" : "<gray>") + type.display() + "排行",
                    active ? "<yellow>当前排序方式</yellow>" : "<gray>点击切换</gray>").build();
            setItem(slot, item, event -> plugin.getGuiManager().openTop(viewer, type, 1));
            slot++;
        }

        for (int index = 0; index < PER_PAGE && start + index < guilds.size(); index++) {
            Guild guild = guilds.get(start + index);
            Placeholders placeholders = Placeholders.of()
                    .put("rank", start + index + 1)
                    .put("guild", guild.getName())
                    .put("leader", guild.getLeaderName())
                    .put("level", guild.getLevel())
                    .put("members", guild.getMemberCount())
                    .put("active", guild.getActive())
                    .put("funds", TimeUtil.money(guild.getFunds()));

            ItemStack item = plugin.getGuiConfig()
                    .template("top.entry-item", GuiUtil.def("GOLDEN_HELMET", "<yellow>#{rank}</yellow> <yellow>{guild}</yellow>",
                            "<gray>会长: <yellow>{leader}</yellow></gray>",
                            "<gray>等级: <yellow>{level}</yellow></gray>",
                            "<gray>成员: <yellow>{members}</yellow></gray>",
                            "<gray>活跃: <yellow>{active}</yellow></gray>",
                            "<gray>资金: <yellow>{funds}</yellow></gray>"))
                    .build(placeholders);
            setItem(GuiUtil.CONTENT_SLOTS[index], item, null);
        }

        addPageButtons(current, pages, newPage -> plugin.getGuiManager().openTop(viewer, sort, newPage));
        addCloseButton();
        fillEmpty();
    }
}
