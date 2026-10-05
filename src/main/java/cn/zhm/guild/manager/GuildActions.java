package cn.zhm.guild.manager;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.ApplicationType;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 公会业务动作, 由命令层与 GUI 层共用, 统一负责消息提示。
 */
public class GuildActions {

    private final ZHMguildPlugin plugin;

    public GuildActions(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    private GuildManager manager() {
        return plugin.getGuildManager();
    }

    /** 校验操作者是否拥有指定职位。 */
    public boolean requireRole(CommandSender sender, Guild guild, GuildRole required) {
        GuildMember self = manager().getMember(sender instanceof Player player ? player.getUniqueId() : null);
        if (guild == null || self == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return false;
        }
        if (!self.getRole().atLeast(required)) {
            plugin.getMessages().send(sender, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(required)));
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------
    // 成员
    // ---------------------------------------------------------

    /** 退出公会。 */
    public boolean leave(Player player) {
        Guild guild = manager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(player, "common.not-in-guild");
            return false;
        }
        GuildMember member = manager().getMember(player.getUniqueId());
        if (member != null && member.isLeader()) {
            plugin.getMessages().send(player, "leave.leader-cannot-leave");
            return false;
        }
        manager().removeMember(guild, player.getUniqueId());
        plugin.getMessages().send(player, "leave.success", Placeholders.of().put("guild", guild.getName()));
        broadcast("leave.broadcast", Placeholders.of().put("player", player.getName()), guild);
        return true;
    }

    /** 踢出成员。 */
    public boolean kick(Player operator, Guild guild, GuildMember target) {
        if (!requireRole(operator, guild, GuildRole.VICE)) {
            return false;
        }
        GuildMember self = manager().getMember(operator.getUniqueId());
        if (target == null) {
            plugin.getMessages().send(operator, "common.target-not-in-guild");
            return false;
        }
        if (target.getUuid().equals(operator.getUniqueId())) {
            plugin.getMessages().send(operator, "kick.cannot-kick-self");
            return false;
        }
        if (target.isLeader()) {
            plugin.getMessages().send(operator, "kick.cannot-kick-leader");
            return false;
        }
        if (!self.isLeader() && !self.getRole().above(target.getRole())) {
            plugin.getMessages().send(operator, "kick.role-too-low");
            return false;
        }
        manager().removeMember(guild, target.getUuid());
        plugin.getMessages().send(operator, "kick.success", Placeholders.of().put("player", target.getName()));
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            plugin.getMessages().send(online, "kick.target", Placeholders.of().put("guild", guild.getName()));
        }
        broadcast("kick.broadcast", Placeholders.of()
                .put("player", target.getName())
                .put("operator", operator.getName()), guild);
        return true;
    }

    /** 提升职位。 */
    public boolean promote(Player operator, Guild guild, GuildMember target) {
        if (!requireRole(operator, guild, GuildRole.VICE)) {
            return false;
        }
        GuildRole next = target.getRole().promote();
        if (next == null) {
            plugin.getMessages().send(operator, "role.already-highest");
            return false;
        }
        GuildMember self = manager().getMember(operator.getUniqueId());
        if (next == GuildRole.LEADER) {
            plugin.getMessages().send(operator, "role.already-highest");
            return false;
        }
        if (!self.isLeader() && !self.getRole().above(next)) {
            plugin.getMessages().send(operator, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.LEADER)));
            return false;
        }
        target.setRole(next);
        manager().saveMember(guild, target);
        plugin.getMessages().send(operator, "role.promote", Placeholders.of()
                .put("player", target.getName())
                .putRaw("role", plugin.getConfigManager().roleDisplay(next)));
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            plugin.getMessages().send(online, "role.notify", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(next)));
        }
        return true;
    }

    /** 降低职位。 */
    public boolean demote(Player operator, Guild guild, GuildMember target) {
        if (!requireRole(operator, guild, GuildRole.LEADER)) {
            return false;
        }
        if (target.isLeader()) {
            plugin.getMessages().send(operator, "role.already-lowest");
            return false;
        }
        GuildRole next = target.getRole().demote();
        if (next == null) {
            plugin.getMessages().send(operator, "role.already-lowest");
            return false;
        }
        target.setRole(next);
        manager().saveMember(guild, target);
        plugin.getMessages().send(operator, "role.demote", Placeholders.of()
                .put("player", target.getName())
                .putRaw("role", plugin.getConfigManager().roleDisplay(next)));
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            plugin.getMessages().send(online, "role.notify", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(next)));
        }
        return true;
    }

    /** 直接设置职位(管理员)。 */
    public boolean setRole(CommandSender sender, Guild guild, GuildMember target, GuildRole role) {
        if (target == null || role == null) {
            return false;
        }
        if (role == GuildRole.LEADER) {
            GuildMember old = guild.findLeaderMember();
            if (old != null && !old.getUuid().equals(target.getUuid())) {
                old.setRole(GuildRole.VICE);
                manager().saveMember(guild, old);
            }
            guild.setLeader(target.getUuid());
            guild.setLeaderName(target.getName());
            manager().saveGuild(guild);
        }
        target.setRole(role);
        manager().saveMember(guild, target);
        plugin.getMessages().send(sender, "role.set", Placeholders.of()
                .put("player", target.getName())
                .putRaw("role", plugin.getConfigManager().roleDisplay(role)));
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            plugin.getMessages().send(online, "role.notify", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(role)));
        }
        return true;
    }

    /** 转让会长。 */
    public boolean transfer(Player operator, Guild guild, GuildMember target) {
        GuildMember self = manager().getMember(operator.getUniqueId());
        if (self == null || !self.isLeader()) {
            plugin.getMessages().send(operator, "common.not-leader");
            return false;
        }
        if (target == null) {
            plugin.getMessages().send(operator, "common.target-not-in-guild");
            return false;
        }
        if (target.getUuid().equals(operator.getUniqueId())) {
            plugin.getMessages().send(operator, "kick.cannot-kick-self");
            return false;
        }
        self.setRole(GuildRole.VICE);
        target.setRole(GuildRole.LEADER);
        guild.setLeader(target.getUuid());
        guild.setLeaderName(target.getName());
        manager().saveMember(guild, self);
        manager().saveMember(guild, target);
        manager().saveGuild(guild);
        plugin.getMessages().send(operator, "role.transfer", Placeholders.of().put("player", target.getName()));
        Player online = Bukkit.getPlayer(target.getUuid());
        if (online != null) {
            plugin.getMessages().send(online, "role.transfer-target", Placeholders.of().put("guild", guild.getName()));
        }
        broadcast("role.transfer-broadcast", Placeholders.of().put("player", target.getName()), guild);
        return true;
    }

    // ---------------------------------------------------------
    // 申请 / 邀请
    // ---------------------------------------------------------

    /** 申请加入公会。 */
    public boolean apply(Player player, Guild guild) {
        if (guild == null) {
            plugin.getMessages().send(player, "common.guild-not-found", Placeholders.of().put("guild", "?"));
            return false;
        }
        if (manager().hasGuild(player.getUniqueId())) {
            plugin.getMessages().send(player, "common.already-in-guild", Placeholders.of()
                    .put("guild", manager().getGuildByPlayer(player.getUniqueId()).getName()));
            return false;
        }
        GuildApplication existing = guild.getApplication(player.getUniqueId());
        if (existing != null) {
            plugin.getMessages().send(player, "join.already-applied", Placeholders.of().put("guild", guild.getName()));
            return false;
        }
        if (manager().countApplications(player.getUniqueId()) >= plugin.getConfigManager().maxApplications()) {
            plugin.getMessages().send(player, "join.max-applications", Placeholders.of()
                    .put("max", plugin.getConfigManager().maxApplications()));
            return false;
        }
        if (guild.getMemberCount() >= manager().maxMembers(guild)) {
            plugin.getMessages().send(player, "join.guild-full");
            return false;
        }
        manager().addApplication(guild, player.getUniqueId(), player.getName(), ApplicationType.APPLY);
        plugin.getMessages().send(player, "join.applied", Placeholders.of().put("guild", guild.getName()));
        notifyManagers(guild, "join.notify-admin", Placeholders.of()
                .put("player", player.getName())
                .put("guild", guild.getName()));
        return true;
    }

    /** 邀请玩家加入公会。 */
    public boolean invite(Player operator, Guild guild, Player target) {
        if (!requireRole(operator, guild, GuildRole.VICE)) {
            return false;
        }
        if (manager().hasGuild(target.getUniqueId())) {
            plugin.getMessages().send(operator, "common.target-in-guild", Placeholders.of()
                    .put("player", target.getName())
                    .put("guild", manager().getGuildByPlayer(target.getUniqueId()).getName()));
            return false;
        }
        if (guild.getApplication(target.getUniqueId()) != null) {
            plugin.getMessages().send(operator, "invite.already-invited");
            return false;
        }
        manager().addApplication(guild, target.getUniqueId(), target.getName(), ApplicationType.INVITE);
        plugin.getMessages().send(operator, "invite.sent", Placeholders.of()
                .put("player", target.getName())
                .put("guild", guild.getName()));
        plugin.getMessages().send(target, "invite.received", Placeholders.of()
                .put("player", operator.getName())
                .put("guild", guild.getName()));
        return true;
    }

    /** 同意某人的入会申请。 */
    public boolean acceptApplication(Player operator, Guild guild, UUID applicant) {
        if (!requireRole(operator, guild, GuildRole.VICE)) {
            return false;
        }
        GuildApplication application = guild.getApplication(applicant);
        if (application == null) {
            plugin.getMessages().send(operator, "join.nobody-applied");
            return false;
        }
        boolean added = manager().addMember(guild, applicant, application.getName(), GuildRole.MEMBER, operator);
        if (added) {
            plugin.getMessages().send(operator, "join.accepted", Placeholders.of().put("player", application.getName()));
            Player online = Bukkit.getPlayer(applicant);
            if (online != null) {
                plugin.getMessages().send(online, "join.accepted-target", Placeholders.of().put("guild", guild.getName()));
            }
        }
        return added;
    }

    /** 拒绝某人的入会申请。 */
    public boolean denyApplication(Player operator, Guild guild, UUID applicant) {
        if (!requireRole(operator, guild, GuildRole.VICE)) {
            return false;
        }
        GuildApplication application = guild.getApplication(applicant);
        if (application == null) {
            plugin.getMessages().send(operator, "join.nobody-applied");
            return false;
        }
        manager().removeApplication(guild, applicant);
        plugin.getMessages().send(operator, "join.denied", Placeholders.of().put("player", application.getName()));
        Player online = Bukkit.getPlayer(applicant);
        if (online != null) {
            plugin.getMessages().send(online, "join.denied-target", Placeholders.of().put("guild", guild.getName()));
        }
        return true;
    }

    /** 同意收到的邀请。 */
    public boolean acceptInvite(Player player, Guild guild) {
        GuildApplication application = guild == null ? null : guild.getApplication(player.getUniqueId());
        if (application == null || application.getType() != ApplicationType.INVITE) {
            plugin.getMessages().send(player, "invite.no-invite", Placeholders.of()
                    .put("guild", guild == null ? "?" : guild.getName()));
            return false;
        }
        if (manager().addMember(guild, player.getUniqueId(), player.getName(), GuildRole.MEMBER, player)) {
            plugin.getMessages().send(player, "invite.joined", Placeholders.of().put("guild", guild.getName()));
            return true;
        }
        return false;
    }

    private static final class ManagerResult {
    }

    // ---------------------------------------------------------
    // 贡献 / 资金
    // ---------------------------------------------------------

    /** 贡献金币给公会。 */
    public boolean contribute(Player player, Guild guild, GuildMember member, double amount) {
        if (guild == null || member == null) {
            plugin.getMessages().send(player, "common.not-in-guild");
            return false;
        }
        if (amount < plugin.getConfigManager().contributeMin()) {
            plugin.getMessages().send(player, "contribute.min", Placeholders.of()
                    .put("min", TimeUtil.money(plugin.getConfigManager().contributeMin())));
            return false;
        }
        if (plugin.getEconomyHook().isEnabled()) {
            if (!plugin.getEconomyHook().has(player, amount)) {
                plugin.getMessages().send(player, "contribute.no-money", Placeholders.of()
                        .put("amount", TimeUtil.money(amount)));
                return false;
            }
            plugin.getEconomyHook().withdraw(player, amount);
        }
        guild.addFunds(amount);
        member.addContribution(amount);
        manager().saveGuild(guild);
        manager().saveMember(guild, member);
        plugin.getMessages().send(player, "contribute.success", Placeholders.of()
                .put("amount", TimeUtil.money(amount))
                .put("contribution", TimeUtil.money(amount)));
        broadcast("contribute.broadcast", Placeholders.of()
                .put("player", player.getName())
                .put("amount", TimeUtil.money(amount)), guild);
        return true;
    }

    // ---------------------------------------------------------
    // 主城
    // ---------------------------------------------------------

    /** 设置公会主城。 */
    public boolean setHome(Player player, Guild guild, GuildMember member) {
        if (guild == null || member == null) {
            plugin.getMessages().send(player, "common.not-in-guild");
            return false;
        }
        GuildRole required = plugin.getConfigManager().homeSetPermission();
        if (!member.getRole().atLeast(required)) {
            plugin.getMessages().send(player, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(required)));
            return false;
        }
        manager().setHome(guild, player.getLocation());
        plugin.getMessages().send(player, "home.set");
        return true;
    }

    /** 传送回公会主城。 */
    public boolean teleportHome(Player player, Guild guild) {
        if (guild == null) {
            plugin.getMessages().send(player, "common.not-in-guild");
            return false;
        }
        Location home = guild.getSafeHome();
        if (home == null) {
            plugin.getMessages().send(player, guild.getHome() == null ? "home.not-set" : "home.world-invalid");
            return false;
        }
        long cooldown = plugin.getTeleportManager().cooldownLeft(player.getUniqueId());
        if (cooldown > 0) {
            plugin.getMessages().send(player, "home.cooldown", Placeholders.of()
                    .put("time", cooldown / 1000 + 1));
            return false;
        }
        plugin.getTeleportManager().setCooldown(player.getUniqueId(),
                plugin.getConfigManager().homeTeleportCooldown());
        plugin.getTeleportManager().teleport(player, home);
        return true;
    }

    // ---------------------------------------------------------
    // 设置
    // ---------------------------------------------------------

    /** 修改公会公告。 */
    public boolean setNotice(Player player, Guild guild, String notice) {
        if (!requireRole(player, guild, GuildRole.VICE)) {
            return false;
        }
        guild.setNotice(Text.safe(notice, 120));
        manager().saveGuild(guild);
        plugin.getMessages().send(player, "notice.set");
        broadcast("notice.broadcast", Placeholders.of().putRaw("notice", guild.getNotice()), guild);
        return true;
    }

    /** 修改公会名称。 */
    public boolean rename(Player player, Guild guild, String newName) {
        GuildMember self = manager().getMember(player.getUniqueId());
        if (self == null || !self.isLeader()) {
            plugin.getMessages().send(player, "common.not-leader");
            return false;
        }
        if (!manager().isValidName(newName, player)) {
            return false;
        }
        boolean colorAllowed = plugin.getConfigManager().allowColorName()
                && player.hasPermission("zhmguild.create.color");
        String name = colorAllowed ? newName.trim() : manager().plainName(newName);
        Guild existing = manager().getGuildByName(manager().plainName(name));
        if (existing != null && existing.getId() != guild.getId()) {
            plugin.getMessages().send(player, "common.guild-name-taken", Placeholders.of().put("guild", name));
            return false;
        }
        double cost = plugin.getConfigManager().renameCost();
        if (cost > 0 && plugin.getEconomyHook().isEnabled()) {
            if (!plugin.getEconomyHook().has(player, cost)) {
                plugin.getMessages().send(player, "settings.rename-no-money", Placeholders.of()
                        .put("money", TimeUtil.money(cost)));
                return false;
            }
            plugin.getEconomyHook().withdraw(player, cost);
        }
        String old = guild.getName();
        guild.setName(name);
        manager().renameIndex(guild, old);
        manager().saveGuild(guild);
        plugin.getMessages().send(player, "settings.renamed", Placeholders.of().put("guild", name));
        return true;
    }

    /** 使用手中物品设置公会图标。 */
    public boolean setIcon(Player player, Guild guild) {
        if (!requireRole(player, guild, GuildRole.VICE)) {
            return false;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            plugin.getMessages().send(player, "settings.icon-hand-empty");
            return false;
        }
        guild.setIcon(hand.getType().name());
        manager().saveGuild(guild);
        plugin.getMessages().send(player, "settings.icon-set", Placeholders.of().put("item", hand.getType().name()));
        return true;
    }

    /** 切换公会 PVP 开关。 */
    public boolean togglePvp(Player player, Guild guild) {
        if (!requireRole(player, guild, GuildRole.VICE)) {
            return false;
        }
        guild.setPvp(!guild.isPvp());
        manager().saveGuild(guild);
        plugin.getMessages().send(player, guild.isPvp() ? "settings.pvp-on" : "settings.pvp-off");
        return true;
    }

    /** 解散公会。 */
    public boolean dissolve(Player player, Guild guild) {
        GuildMember self = manager().getMember(player.getUniqueId());
        if (self == null || !self.isLeader()) {
            plugin.getMessages().send(player, "common.not-leader");
            return false;
        }
        manager().dissolveGuild(guild);
        plugin.getMessages().send(player, "dissolve.success", Placeholders.of().put("guild", guild.getName()));
        Bukkit.getServer().sendMessage(plugin.getMessages().parse(
                plugin.getMessages().str("dissolve.broadcast", Placeholders.of().put("guild", guild.getName())), player));
        return true;
    }

    /** 切换公会聊天频道。 */
    public boolean toggleChat(Player player, GuildMember member) {
        if (member == null) {
            plugin.getMessages().send(player, "common.not-in-guild");
            return false;
        }
        member.setChatToggled(!member.isChatToggled());
        plugin.getMessages().send(player, member.isChatToggled() ? "chat.toggled-on" : "chat.toggled-off");
        return true;
    }

    // ---------------------------------------------------------
    // 工具
    // ---------------------------------------------------------

    /** 给公会内所有在线管理者发送消息。 */
    public void notifyManagers(Guild guild, String path, Placeholders placeholders) {
        for (GuildMember member : guild.getMembers()) {
            if (!member.getRole().atLeast(GuildRole.VICE)) {
                continue;
            }
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null) {
                plugin.getMessages().send(online, path, placeholders);
            }
        }
    }

    /** 给公会内所有在线成员广播。 */
    public void broadcast(String path, Placeholders placeholders, Guild guild) {
        if (guild == null || !plugin.getMessages().has(path)) {
            return;
        }
        String raw = plugin.getMessages().str(path, placeholders);
        for (GuildMember member : guild.getMembers()) {
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null) {
                online.sendMessage(plugin.getMessages().parse(raw, online));
            }
        }
    }
}
