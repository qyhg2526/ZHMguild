package cn.zhm.guild.hook;

import cn.zhm.guild.ZHMguildPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 经济挂钩。未安装 Vault 时 isEnabled() 返回 false, 相关功能自动降级。
 */
public class EconomyHook {

    private final ZHMguildPlugin plugin;
    private Economy economy;

    public EconomyHook(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            RegisteredServiceProvider<Economy> registration =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration == null) {
                return false;
            }
            economy = registration.getProvider();
            return economy != null;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Vault 经济挂钩失败: " + throwable.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isEnabled() || player == null) {
            return 0.0D;
        }
        try {
            return economy.getBalance(player);
        } catch (Throwable throwable) {
            return 0.0D;
        }
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!isEnabled() || player == null) {
            return false;
        }
        try {
            return economy.has(player, amount);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isEnabled() || player == null || amount <= 0) {
            return true;
        }
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Throwable throwable) {
            return false;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isEnabled() || player == null || amount <= 0) {
            return false;
        }
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response != null && response.transactionSuccess();
        } catch (Throwable throwable) {
            return false;
        }
    }

    public String format(double amount) {
        if (!isEnabled()) {
            return String.valueOf(amount);
        }
        try {
            return economy.format(amount);
        } catch (Throwable throwable) {
            return String.valueOf(amount);
        }
    }
}
