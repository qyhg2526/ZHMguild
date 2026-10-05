package cn.zhm.guild.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * GUI 点击事件分发。
 */
public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // 始终以视图顶层容器为准, 避免 getInventory() 语义差异带来的问题
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Gui gui)) {
            return;
        }
        // 任何点击都拦截, 防止菜单物品被取出
        event.setCancelled(true);

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked != top) {
            return;
        }
        if (event.getWhoClicked() != gui.viewer()) {
            return;
        }
        gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Gui) {
            event.setCancelled(true);
        }
    }
}
