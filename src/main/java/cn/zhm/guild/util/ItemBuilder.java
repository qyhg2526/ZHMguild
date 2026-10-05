package cn.zhm.guild.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品构建工具。
 */
public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material == null ? Material.STONE : material));
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(new ItemStack(material == null ? Material.STONE : material, Math.max(1, Math.min(64, amount))));
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack == null ? new ItemStack(Material.STONE) : stack.clone());
    }

    public static ItemBuilder of(String materialName) {
        Material material = Material.matchMaterial(materialName == null ? "STONE" : materialName.toUpperCase());
        return of(material == null ? Material.STONE : material);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(Component name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        return name(Text.mm(miniMessage));
    }

    public ItemBuilder lore(List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore.isEmpty() ? null : new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder loreRaw(List<String> miniMessageLore) {
        return lore(Text.mmList(miniMessageLore));
    }

    public ItemBuilder glow(boolean glow) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder hideAll() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder skullOwner(org.bukkit.OfflinePlayer owner) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && owner != null) {
            skullMeta.setOwningPlayer(owner);
            item.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemBuilder modelData(Integer data) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && data != null) {
            meta.setCustomModelData(data);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 写入一个隐藏标记, 用于识别 GUI 物品。 */
    public ItemBuilder tag(Plugin plugin, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }
}
