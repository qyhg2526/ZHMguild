package cn.zhm.guild.gui;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.config.GuiConfig;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI 基类。所有菜单都继承此类, 由 {@link GuiListener} 分发点击事件。
 */
public abstract class Gui implements InventoryHolder {

    protected final ZHMguildPlugin plugin;
    protected final Player viewer;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;
    private boolean refreshScheduled;

    protected Gui(ZHMguildPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** 菜单标题。 */
    protected abstract Component title();

    /** 菜单大小(9 的倍数)。 */
    protected abstract int size();

    /** 构建菜单内容。 */
    protected abstract void build();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** 该菜单的查看者。 */
    public Player viewer() {
        return viewer;
    }

    /** 打开菜单。 */
    public void open() {
        this.actions.clear();
        this.inventory = Bukkit.createInventory(this, size(), title());
        build();
        viewer.openInventory(inventory);
    }

    /** 在当前界面上重新渲染(下一 tick 执行, 避免事件期间修改背包导致客户端不同步)。 */
    public void refresh() {
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            refreshScheduled = false;
            if (!viewer.isOnline() || inventory == null) {
                return;
            }
            if (viewer.getOpenInventory().getTopInventory() != inventory) {
                return;
            }
            actions.clear();
            inventory.clear();
            build();
        });
    }

    protected Component titleOf(String menu, String fallback, Placeholders placeholders) {
        String raw = plugin.getGuiConfig().title(menu, fallback);
        if (placeholders != null) {
            raw = placeholders.apply(raw);
        }
        return Text.mm(raw);
    }

    /** 放置一个物品并绑定点击动作。 */
    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    /** 放置一个来自 gui.yml 的按钮。 */
    protected void button(String menu, String key, GuiConfig.GuiItemDef fallback,
                          Placeholders placeholders, Consumer<InventoryClickEvent> action) {
        GuiConfig.GuiItemDef def = plugin.getGuiConfig().itemOr(menu, key, fallback);
        if (def == null) {
            return;
        }
        setItem(def.slot(), def.build(placeholders == null ? Placeholders.of() : placeholders), action);
    }

    /** 填充所有空位。 */
    protected void fillEmpty() {
        GuiConfig.GuiItemDef filler = plugin.getGuiConfig().common("filler");
        ItemStack item = filler != null
                ? filler.build()
                : cn.zhm.guild.util.ItemBuilder.of(org.bukkit.Material.GRAY_STAINED_GLASS_PANE)
                .name(" ").hideAll().build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    /** 添加返回按钮。 */
    protected void addBackButton(Runnable action) {
        GuiConfig.GuiItemDef def = plugin.getGuiConfig().common("back");
        int slot = plugin.getGuiConfig().commonSlot("back", 49);
        ItemStack item = def != null ? def.build() : cn.zhm.guild.util.ItemBuilder
                .of(org.bukkit.Material.ARROW).name("<yellow>返回</yellow>").hideAll().build();
        setItem(slot, item, event -> action.run());
    }

    /** 添加关闭按钮。 */
    protected void addCloseButton() {
        GuiConfig.GuiItemDef def = plugin.getGuiConfig().common("close");
        int slot = plugin.getGuiConfig().commonSlot("close", 53);
        ItemStack item = def != null ? def.build() : cn.zhm.guild.util.ItemBuilder
                .of(org.bukkit.Material.BARRIER).name("<red>关闭</red>").hideAll().build();
        setItem(slot, item, event -> viewer.closeInventory());
    }

    /** 添加翻页按钮。 */
    protected void addPageButtons(int page, int pages, Consumer<Integer> onPage) {
        Placeholders placeholders = Placeholders.of().put("page", page).put("pages", pages);
        GuiConfig.GuiItemDef previous = plugin.getGuiConfig().common("previous");
        GuiConfig.GuiItemDef next = plugin.getGuiConfig().common("next");
        if (previous != null && page > 1) {
            setItem(previous.slot(), previous.build(placeholders), event -> onPage.accept(page - 1));
        }
        if (next != null && page < pages) {
            setItem(next.slot(), next.build(placeholders), event -> onPage.accept(page + 1));
        }
    }

    /** 处理点击。 */
    public void handleClick(InventoryClickEvent event) {
        if (inventory == null || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        Consumer<InventoryClickEvent> action = actions.get(event.getSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    /** 左键判断。 */
    protected static boolean isLeft(InventoryClickEvent event) {
        return event.isLeftClick();
    }

    /** 右键判断。 */
    protected static boolean isRight(InventoryClickEvent event) {
        return event.isRightClick();
    }

    /** 潜行点击判断。 */
    protected static boolean isShift(InventoryClickEvent event) {
        return event.isShiftClick();
    }
}
