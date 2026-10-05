package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Arena;
import cn.zhm.guild.model.GuildWar;
import cn.zhm.guild.util.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会战场地列表(管理员可传送与删除)。
 */
public class WarArenaMenu extends Gui {

    private static final int PER_PAGE = GuiUtil.CONTENT_SLOTS.length;

    private final int page;

    public WarArenaMenu(ZHMguildPlugin plugin, Player viewer, int page) {
        super(plugin, viewer);
        this.page = Math.max(1, page);
    }

    @Override
    protected Component title() {
        return titleOf("war-arena", "<dark_gray>公会战场地</dark_gray>", Placeholders.of());
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("war-arena", 54);
    }

    @Override
    protected void build() {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        int pages = GuiUtil.pages(arenas.size(), PER_PAGE);
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;
        boolean admin = viewer.hasPermission("zhmguild.admin");

        if (arenas.isEmpty()) {
            setItem(22, GuiUtil.def(22, "BARRIER", "<red>还没有任何场地</red>",
                            "<gray>请管理员站到场地中执行:</gray>",
                            "<yellow>/zg setLocation mate &lt;场地名&gt; 1</yellow>",
                            "<yellow>/zg setLocation mate &lt;场地名&gt; 2</yellow>",
                            "<yellow>/zg setLocation mate &lt;场地名&gt; 3</yellow> <gray>(观看点, 可选)</gray>")
                    .build(), null);
        }

        for (int index = 0; index < PER_PAGE && start + index < arenas.size(); index++) {
            Arena arena = arenas.get(start + index);
            String state = arena.isInUse() ? "<red>使用中</red>"
                    : arena.isReady() ? "<green>空闲</green>" : "<yellow>缺少出生点</yellow>";
            if (arena.isInUse()) {
                GuildWar war = plugin.getGuildWarManager().getWar(arena.getOccupiedBy());
                if (war != null) {
                    state = "<red>" + war.getRedGuild().getName() + "</red> <gray>VS</gray> <blue>"
                            + war.getBlueGuild().getName() + "</blue>";
                }
            }
            Placeholders placeholders = Placeholders.of()
                    .put("arena", arena.getName())
                    .putRaw("state", state)
                    .put("red", GuiUtil.describe(arena.getRedSpawn()))
                    .put("blue", GuiUtil.describe(arena.getBlueSpawn()))
                    .put("spectate", GuiUtil.describe(arena.getSpectateSpawn()));
            ItemStack item = plugin.getGuiConfig()
                    .template("war-arena.arena-item", GuiUtil.def("GRASS_BLOCK", "<yellow>{arena}</yellow>",
                            "<gray>状态: {state}</gray>",
                            "<gray>红队: <yellow>{red}</yellow></gray>",
                            "<gray>蓝队: <yellow>{blue}</yellow></gray>",
                            "<gray>观看点: <yellow>{spectate}</yellow></gray>"))
                    .build(placeholders);
            setItem(GuiUtil.CONTENT_SLOTS[index], item, event -> {
                if (!admin) {
                    plugin.getMessages().send(viewer, "common.no-permission");
                    return;
                }
                if (event.isShiftClick()) {
                    if (arena.isInUse()) {
                        plugin.getMessages().send(viewer, "war.arena-in-use");
                        return;
                    }
                    plugin.getGuiManager().openConfirm(viewer,
                            plugin.getMessages().str("war.arena-delete-confirm", Placeholders.of().put("arena", arena.getName())),
                            () -> {
                                plugin.getArenaManager().remove(arena.getName());
                                plugin.getMessages().send(viewer, "war.arena-deleted", Placeholders.of().put("arena", arena.getName()));
                                plugin.getGuiManager().openWarArena(viewer, current);
                            });
                    return;
                }
                Location target = arena.getRedSpawn();
                if (target == null || target.getWorld() == null) {
                    plugin.getMessages().send(viewer, "war.arena-not-ready");
                    return;
                }
                viewer.teleport(target);
                plugin.getMessages().send(viewer, "war.arena-teleported", Placeholders.of().put("arena", arena.getName()));
                viewer.closeInventory();
            });
        }

        addPageButtons(current, pages, newPage -> plugin.getGuiManager().openWarArena(viewer, newPage));
        addBackButton(() -> plugin.getGuiManager().openGuildWar(viewer));
        fillEmpty();
    }
}
