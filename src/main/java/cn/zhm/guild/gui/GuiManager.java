package cn.zhm.guild.gui;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.menu.ApplicationMenu;
import cn.zhm.guild.gui.menu.ConfirmMenu;
import cn.zhm.guild.gui.menu.GuildListMenu;
import cn.zhm.guild.gui.menu.GuildWarMenu;
import cn.zhm.guild.gui.menu.MainMenu;
import cn.zhm.guild.gui.menu.MemberListMenu;
import cn.zhm.guild.gui.menu.MemberManageMenu;
import cn.zhm.guild.gui.menu.NoGuildMenu;
import cn.zhm.guild.gui.menu.SettingsMenu;
import cn.zhm.guild.gui.menu.TopMenu;
import cn.zhm.guild.gui.menu.WarArenaMenu;
import cn.zhm.guild.gui.menu.WarRewardMenu;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 菜单调度中心。
 */
public class GuiManager {

    private final ZHMguildPlugin plugin;

    public GuiManager(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    /** 在下一 tick 打开菜单, 避免在点击事件中切换界面导致客户端不同步。 */
    public void open(Gui gui) {
        if (gui == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, gui::open);
    }

    /** 打开主菜单(自动判断是否已加入公会)。 */
    public void openMain(Player player) {
        if (plugin.getGuildManager().hasGuild(player.getUniqueId())) {
            open(new MainMenu(plugin, player));
        } else {
            open(new NoGuildMenu(plugin, player));
        }
    }

    public void openNoGuild(Player player) {
        open(new NoGuildMenu(plugin, player));
    }

    public void openGuildList(Player player, SortType sort, int page) {
        open(new GuildListMenu(plugin, player, sort == null ? SortType.LEVEL : sort, page));
    }

    public void openMembers(Player player, int page) {
        open(new MemberListMenu(plugin, player, page));
    }

    public void openMemberManage(Player player, UUID target, int returnPage) {
        open(new MemberManageMenu(plugin, player, target, returnPage));
    }

    public void openApplications(Player player, int page) {
        open(new ApplicationMenu(plugin, player, page));
    }

    public void openSettings(Player player) {
        open(new SettingsMenu(plugin, player));
    }

    public void openGuildWar(Player player) {
        open(new GuildWarMenu(plugin, player));
    }

    public void openWarArena(Player player, int page) {
        open(new WarArenaMenu(plugin, player, page));
    }

    public void openWarReward(Player player) {
        open(new WarRewardMenu(plugin, player));
    }

    public void openTop(Player player, SortType sort, int page) {
        open(new TopMenu(plugin, player, sort == null ? SortType.LEVEL : sort, page));
    }

    public void openConfirm(Player player, String message, Runnable onConfirm) {
        open(new ConfirmMenu(plugin, player, message, onConfirm));
    }

    /** 创建公会流程: 聊天栏输入名称 -> 确认 -> 创建。 */
    public void beginCreate(Player player) {
        if (plugin.getGuildManager().hasGuild(player.getUniqueId())) {
            plugin.getMessages().send(player, "common.already-in-guild", Placeholders.of()
                    .put("guild", plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()).getName()));
            return;
        }
        plugin.getChatInputManager().request(player, "create.input-name", 30, input -> {
            if (!plugin.getGuildManager().isValidName(input, player)) {
                return;
            }
            String name = plugin.getGuildManager().plainName(input);
            Guild existing = plugin.getGuildManager().getGuildByName(name);
            if (existing != null) {
                plugin.getMessages().send(player, "create.name-taken", Placeholders.of().put("guild", name));
                return;
            }
            String message = plugin.getMessages().str("create.confirm-gui", Placeholders.of()
                    .put("guild", name)
                    .put("money", TimeUtil.money(plugin.getConfigManager().createMoney())));
            openConfirm(player, message, () -> {
                Guild created = plugin.getGuildManager().createGuild(player, input);
                if (created != null) {
                    openMain(player);
                }
            });
        });
    }
}
