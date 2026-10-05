package cn.zhm.guild.gui.menu;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.gui.Gui;
import cn.zhm.guild.gui.GuiUtil;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 入会申请处理界面。
 */
public class ApplicationMenu extends Gui {

    private static final int PER_PAGE = GuiUtil.CONTENT_SLOTS.length;

    private final int page;

    public ApplicationMenu(ZHMguildPlugin plugin, Player viewer, int page) {
        super(plugin, viewer);
        this.page = Math.max(1, page);
    }

    @Override
    protected Component title() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        return titleOf("applications", "<dark_gray>入会申请</dark_gray> <gray>»</gray> <yellow>{guild}</yellow>",
                Placeholders.of().put("guild", guild == null ? "" : guild.getName()));
    }

    @Override
    protected int size() {
        return plugin.getGuiConfig().size("applications", 54);
    }

    @Override
    protected void build() {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(viewer.getUniqueId());
        GuildMember self = plugin.getGuildManager().getMember(viewer.getUniqueId());
        if (guild == null || self == null) {
            plugin.getGuiManager().openNoGuild(viewer);
            return;
        }
        if (!self.getRole().atLeast(GuildRole.VICE)) {
            plugin.getMessages().send(viewer, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
            plugin.getGuiManager().openMain(viewer);
            return;
        }

        List<GuildApplication> applications = guild.getApplications();
        int pages = GuiUtil.pages(applications.size(), PER_PAGE);
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;

        for (int index = 0; index < PER_PAGE && start + index < applications.size(); index++) {
            GuildApplication application = applications.get(start + index);
            Placeholders placeholders = Placeholders.of()
                    .put("player", application.getName())
                    .put("type", application.getType().display())
                    .put("time", TimeUtil.formatDateTime(application.getTime()))
                    .put("guild", guild.getName());

            ItemStack item = plugin.getGuiConfig()
                    .template("applications.application-item", GuiUtil.def("PAPER", "<yellow>{player}</yellow>",
                            "<gray>类型: <yellow>{type}</yellow></gray>",
                            "<gray>时间: <yellow>{time}</yellow></gray>",
                            "", "<green>» 左键同意</green>", "<red>» 右键拒绝</red>"))
                    .build(placeholders);

            setItem(GuiUtil.CONTENT_SLOTS[index], item, event -> {
                if (event.isRightClick()) {
                    plugin.getGuildActions().denyApplication(viewer, guild, application.getUuid());
                } else {
                    plugin.getGuildActions().acceptApplication(viewer, guild, application.getUuid());
                }
                refresh();
            });
        }

        addPageButtons(current, pages, newPage -> plugin.getGuiManager().openApplications(viewer, newPage));
        addBackButton(() -> plugin.getGuiManager().openMain(viewer));
        fillEmpty();
    }
}
