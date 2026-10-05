package cn.zhm.guild.command;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.manager.ArenaManager;
import cn.zhm.guild.model.ApplicationType;
import cn.zhm.guild.model.Arena;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildApplication;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.GuildRole;
import cn.zhm.guild.model.GuildWar;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /zhmguild 命令分发器。
 */
public class GuildCommand implements CommandExecutor, TabCompleter {

    private final ZHMguildPlugin plugin;

    public GuildCommand(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getGuiManager().openMain(player);
            } else {
                help(sender);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "help", "?" -> help(sender);
                case "reload" -> reload(sender);
                case "open", "list" -> open(sender, args);
                case "me" -> me(sender);
                case "create" -> create(sender, args);
                case "join" -> join(sender, args);
                case "accept" -> accept(sender, args);
                case "deny" -> deny(sender, args);
                case "invitation", "invite" -> invitation(sender, args);
                case "leave", "quit" -> leave(sender);
                case "up", "upgrade" -> up(sender);
                case "spawn", "home" -> spawn(sender);
                case "sethome", "setspawn" -> setSpawn(sender);
                case "signin", "sign" -> signIn(sender);
                case "contribute", "donate" -> contribute(sender, args);
                case "top", "rank" -> top(sender, args);
                case "members" -> members(sender, args);
                case "info" -> info(sender, args);
                case "notice" -> notice(sender, args);
                case "kick" -> kick(sender, args);
                case "promote" -> promote(sender, args);
                case "demote" -> demote(sender, args);
                case "transfer" -> transfer(sender, args);
                case "pvp" -> pvp(sender);
                case "dissolve" -> dissolve(sender, args);
                case "confirm" -> confirm(sender);
                case "cancel" -> cancel(sender);
                case "view" -> view(sender, args);
                case "give" -> modify(sender, args, "give");
                case "take" -> modify(sender, args, "take");
                case "set" -> modify(sender, args, "set");
                case "admincreate" -> adminCreate(sender, args);
                case "admineditguildname" -> adminEditName(sender, args);
                case "setrole" -> setRole(sender, args);
                case "adminup" -> adminUp(sender, args);
                case "refresh" -> refresh(sender, args);
                case "clear" -> clear(sender, args);
                case "reward" -> reward(sender, args);
                case "debug" -> debug(sender, args);
                case "setlocation" -> setLocation(sender, args);
                case "war", "guildwar" -> war(sender, args);
                default -> plugin.getMessages().send(sender, "common.unknown-command");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("执行命令 /" + label + " " + String.join(" ", args) + " 时出错: " + exception);
            if (plugin.getConfigManager().debug()) {
                exception.printStackTrace();
            }
        }
        return true;
    }

    // ---------------------------------------------------------
    // 工具
    // ---------------------------------------------------------

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.getMessages().send(sender, "common.player-only");
        return null;
    }

    private boolean has(CommandSender sender, String permission) {
        if (sender.hasPermission("zhmguild.admin") || sender.hasPermission(permission)) {
            return true;
        }
        plugin.getMessages().send(sender, "common.no-permission");
        return false;
    }

    private Double number(CommandSender sender, String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            plugin.getMessages().send(sender, "common.invalid-number");
            return null;
        }
    }

    private boolean usage(CommandSender sender, String usage) {
        plugin.getMessages().send(sender, "common.usage", Placeholders.of().put("usage", usage));
        return true;
    }

    // ---------------------------------------------------------
    // 基础命令
    // ---------------------------------------------------------

    private void help(CommandSender sender) {
        plugin.getMessages().sendRaw(sender, "help.header", null);
        listHelp(sender, "/zg open [排序]", "打开公会界面", null);
        listHelp(sender, "/zg me", "打开我的公会", null);
        listHelp(sender, "/zg create [名称]", "创建公会", "zhmguild.create");
        listHelp(sender, "/zg join <公会名>", "申请加入公会", null);
        listHelp(sender, "/zg accept [公会名]", "同意入会申请 / 邀请", null);
        listHelp(sender, "/zg deny [公会名]", "拒绝入会申请 / 邀请", null);
        listHelp(sender, "/zg invitation <玩家>", "邀请玩家加入公会", "zhmguild.invitation");
        listHelp(sender, "/zg leave", "退出公会", null);
        listHelp(sender, "/zg up", "升级公会", null);
        listHelp(sender, "/zg spawn", "传送回公会主城", null);
        listHelp(sender, "/zg sethome", "设置公会主城", null);
        listHelp(sender, "/zg signin", "每日签到", null);
        listHelp(sender, "/zg contribute <数量>", "向公会贡献金币", null);
        listHelp(sender, "/zg members", "查看公会成员", null);
        listHelp(sender, "/zg info [公会名]", "查看公会信息", null);
        listHelp(sender, "/zg top [排序]", "查看公会排行", null);
        listHelp(sender, "/zg war", "匹配公会战界面", null);
        listHelp(sender, "/zg war match|cancel", "发起 / 取消公会战匹配", null);
        listHelp(sender, "/zg war status", "查看公会战状态", null);
        listHelp(sender, "/zg notice <内容>", "修改公会公告", null);
        listHelp(sender, "/zg kick <玩家>", "踢出成员", null);
        listHelp(sender, "/zg promote|demote <玩家>", "调整成员职位", null);
        listHelp(sender, "/zg transfer <玩家>", "转让会长", null);
        listHelp(sender, "/zg pvp", "切换公会 PVP", null);
        listHelp(sender, "/zg dissolve", "解散公会", null);
        if (sender.hasPermission("zhmguild.admin")) {
            listHelp(sender, "/zg reload", "重载配置", null);
            listHelp(sender, "/zg view <guild|player|list>", "管理员查看信息", null);
            listHelp(sender, "/zg give|take|set <类型> <名称> <数量>", "修改资金/活跃/贡献", null);
            listHelp(sender, "/zg adminCreate <公会名> [玩家]", "管理员创建公会", null);
            listHelp(sender, "/zg adminEditGuildName <旧> <新>", "管理员改名", null);
            listHelp(sender, "/zg setRole <玩家> <角色>", "设置职位", null);
            listHelp(sender, "/zg adminUp <公会名> [等级]", "强制升级", null);
            listHelp(sender, "/zg refresh [公会名]", "刷新公会数据", null);
            listHelp(sender, "/zg clear <application|guild>", "清理数据", null);
            listHelp(sender, "/zg reward <类型> <玩家> <数量>", "发放奖励", null);
            listHelp(sender, "/zg setLocation mate <场地名> <1|2|3>", "设置公会战场地出生点", null);
            listHelp(sender, "/zg war list", "查看全部公会战场地", null);
            listHelp(sender, "/zg war start <红队> <蓝队> [场地]", "强制开战", null);
            listHelp(sender, "/zg war queue|unqueue <公会名>", "强制入队 / 出队", null);
            listHelp(sender, "/zg war stop [all|ID]", "强制结束公会战", null);
            listHelp(sender, "/zg debug", "自检 GUI 配置与运行状态", null);
        }
        plugin.getMessages().sendRaw(sender, "help.footer", null);
    }

    private void listHelp(CommandSender sender, String command, String description, String permission) {
        if (permission != null && !sender.hasPermission("zhmguild.admin") && !sender.hasPermission(permission)) {
            return;
        }
        plugin.getMessages().sendRaw(sender, "help.line", Placeholders.of()
                .put("command", command)
                .put("desc", description));
    }

    private void reload(CommandSender sender) {
        if (!has(sender, "zhmguild.reload")) {
            return;
        }
        plugin.reloadAll();
        plugin.getMessages().send(sender, "common.reloaded");
    }

    private void open(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length > 1) {
            SortType sort = SortType.parse(args[1], SortType.LEVEL);
            plugin.getGuiManager().openGuildList(player, sort, 1);
            return;
        }
        plugin.getGuiManager().openMain(player);
    }

    private void me(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        plugin.getGuiManager().openMain(player);
    }

    private void create(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.create")) {
            return;
        }
        if (args.length < 2) {
            plugin.getGuiManager().beginCreate(player);
            return;
        }
        if (!plugin.getGuildManager().isValidName(args[1], player)) {
            return;
        }
        String name = plugin.getGuildManager().plainName(args[1]);
        if (plugin.getGuildManager().getGuildByName(name) != null) {
            plugin.getMessages().send(player, "create.name-taken", Placeholders.of().put("guild", name));
            return;
        }
        String message = plugin.getMessages().str("create.confirm-gui", Placeholders.of()
                .put("guild", name)
                .put("money", TimeUtil.money(plugin.getConfigManager().createMoney())));
        plugin.getGuiManager().openConfirm(player, message, () -> plugin.getGuildManager().createGuild(player, args[1]));
    }

    private void join(CommandSender sender, String[] args) {
        if (args.length >= 3 && sender.hasPermission("zhmguild.admin")) {
            forceJoin(sender, args[1], args[2]);
            return;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg join <公会名>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
            return;
        }
        GuildApplication application = guild.getApplication(player.getUniqueId());
        if (application != null && application.getType() == ApplicationType.INVITE) {
            plugin.getGuildActions().acceptInvite(player, guild);
            return;
        }
        plugin.getGuildActions().apply(player, guild);
    }

    private void forceJoin(CommandSender sender, String guildName, String playerName) {
        if (!has(sender, "zhmguild.join")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", guildName));
            return;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        UUID uuid = target != null ? target.getUniqueId() : null;
        String name = target != null ? target.getName() : playerName;
        if (uuid == null) {
            plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", playerName));
            return;
        }
        if (plugin.getGuildManager().addMember(guild, uuid, name, GuildRole.MEMBER, sender)) {
            plugin.getMessages().send(sender, "admin.join-forced", Placeholders.of()
                    .put("player", name)
                    .put("guild", guild.getName()));
        }
    }

    private void accept(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.accept")) {
            return;
        }
        if (args.length >= 2) {
            Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
            if (guild == null) {
                plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
                return;
            }
            plugin.getGuildActions().acceptInvite(player, guild);
            return;
        }
        // 无参数: 优先处理邀请, 其次处理本会申请
        List<Guild> invites = plugin.getGuildManager().invitesOf(player.getUniqueId());
        if (!invites.isEmpty()) {
            plugin.getGuildActions().acceptInvite(player, invites.get(0));
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "invite.no-invite", Placeholders.of().put("guild", "?"));
            return;
        }
        if (guild.getApplicationCount() == 0) {
            plugin.getMessages().send(sender, "join.nobody-applied");
            return;
        }
        GuildApplication application = guild.getApplications().get(0);
        plugin.getGuildActions().acceptApplication(player, guild, application.getUuid());
    }

    private void deny(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.deny")) {
            return;
        }
        if (args.length >= 2) {
            Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
            if (guild == null) {
                plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
                return;
            }
            GuildApplication application = guild.getApplication(player.getUniqueId());
            if (application == null) {
                plugin.getMessages().send(sender, "invite.no-invite", Placeholders.of().put("guild", guild.getName()));
                return;
            }
            plugin.getGuildManager().removeApplication(guild, player.getUniqueId());
            plugin.getMessages().send(sender, "join.denied", Placeholders.of().put("player", player.getName()));
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null || guild.getApplicationCount() == 0) {
            plugin.getMessages().send(sender, "join.nobody-applied");
            return;
        }
        GuildApplication application = guild.getApplications().get(0);
        plugin.getGuildActions().denyApplication(player, guild, application.getUuid());
    }

    private void invitation(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.invitation")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg invitation <玩家>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", args[1]));
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildActions().invite(player, guild, target);
    }

    private void leave(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.leave")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuiManager().openConfirm(player,
                plugin.getMessages().str("leave.confirm", Placeholders.of().put("guild", guild.getName())),
                () -> plugin.getGuildActions().leave(player));
    }

    private void up(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.up")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildManager().upgrade(guild, player);
    }

    private void spawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.spawn")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildActions().teleportHome(player, guild);
    }

    private void setSpawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.setSpawn")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        if (guild == null || member == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildActions().setHome(player, guild, member);
    }

    private void signIn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.signIn")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        if (guild == null || member == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildManager().signIn(player, guild, member);
    }

    private void contribute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.contribute")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg contribute <数量>");
            return;
        }
        Double amount = number(sender, args[1]);
        if (amount == null) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        plugin.getGuildActions().contribute(player, guild, member, amount);
    }

    private void top(CommandSender sender, String[] args) {
        SortType sort = args.length > 1 ? SortType.parse(args[1], SortType.LEVEL) : SortType.LEVEL;
        if (sender instanceof Player player) {
            plugin.getGuiManager().openTop(player, sort, 1);
            return;
        }
        sendTop(sender, sort);
    }

    private void sendTop(CommandSender sender, SortType sort) {
        List<Guild> guilds = plugin.getGuildManager().sortedGuilds(sort);
        int size = Math.min(plugin.getConfigManager().topSize(), guilds.size());
        plugin.getMessages().sendRaw(sender, "gui.top-title", Placeholders.of().put("sort", sort.display()));
        for (int index = 0; index < size; index++) {
            Guild guild = guilds.get(index);
            sender.sendMessage(plugin.getMessages().parse("<gray>#" + (index + 1) + " </gray><yellow>"
                    + guild.getName() + "</yellow> <dark_gray>|</dark_gray> <gray>等级 " + guild.getLevel()
                    + " | 活跃 " + guild.getActive() + " | 成员 " + guild.getMemberCount() + "</gray>", sender));
        }
    }

    private void members(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Guild guild = args.length > 1
                ? plugin.getGuildManager().getGuildByName(args[1])
                : plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of()
                    .put("guild", args.length > 1 ? args[1] : "?"));
            return;
        }
        plugin.getGuiManager().openMembers(player, 1);
    }

    private void info(CommandSender sender, String[] args) {
        Guild guild = args.length > 1
                ? plugin.getGuildManager().getGuildByName(args[1])
                : (sender instanceof Player player ? plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()) : null);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of()
                    .put("guild", args.length > 1 ? args[1] : "?"));
            return;
        }
        sendGuildInfo(sender, guild);
    }

    private void sendGuildInfo(CommandSender sender, Guild guild) {
        Location home = guild.getSafeHome();
        String homeText = home == null ? "未设置"
                : home.getWorld().getName() + " " + (int) home.getX() + "," + (int) home.getY() + "," + (int) home.getZ();
        plugin.getMessages().sendRaw(sender, "admin.view-header", Placeholders.of().put("guild", guild.getName()));
        sender.sendMessage(plugin.getMessages().parse("<gray>会长: <yellow>" + guild.getLeaderName() + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>等级: <yellow>" + guild.getLevel() + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>成员: <yellow>" + guild.getMemberCount() + "</yellow>/<yellow>"
                + plugin.getGuildManager().maxMembers(guild) + "</yellow> | 活跃: <yellow>" + guild.getActive()
                + "</yellow> | 月度活跃: <yellow>" + guild.getMonthActive() + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>资金: <yellow>" + TimeUtil.money(guild.getFunds())
                + "</yellow> | 矿石: <yellow>" + guild.getOre() + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>主城: <yellow>" + homeText + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>公告: <white>"
                + (guild.getNotice().isEmpty() ? "无" : Text.escape(guild.getNotice())) + "</white></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>排行: <yellow>#"
                + plugin.getGuildManager().rank(guild, SortType.LEVEL) + "</yellow></gray>", sender));
    }

    private void notice(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.notice")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        if (args.length < 2) {
            plugin.getChatInputManager().request(player, "notice.input", 30,
                    input -> plugin.getGuildActions().setNotice(player, guild, input));
            return;
        }
        plugin.getGuildActions().setNotice(player, guild, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }

    private void kick(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.kick")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg kick <玩家>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        GuildMember target = guild.getSortedMembers().stream()
                .filter(member -> member.getName().equalsIgnoreCase(args[1]))
                .findFirst().orElse(null);
        if (target == null) {
            plugin.getMessages().send(sender, "kick.target-not-member");
            return;
        }
        plugin.getGuildActions().kick(player, guild, target);
    }

    private void promote(CommandSender sender, String[] args) {
        changeRole(sender, args, true);
    }

    private void demote(CommandSender sender, String[] args) {
        changeRole(sender, args, false);
    }

    private void changeRole(CommandSender sender, String[] args, boolean promote) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg " + (promote ? "promote" : "demote") + " <玩家>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        GuildMember target = guild.getSortedMembers().stream()
                .filter(member -> member.getName().equalsIgnoreCase(args[1]))
                .findFirst().orElse(null);
        if (target == null) {
            plugin.getMessages().send(sender, "kick.target-not-member");
            return;
        }
        if (promote) {
            plugin.getGuildActions().promote(player, guild, target);
        } else {
            plugin.getGuildActions().demote(player, guild, target);
        }
    }

    private void transfer(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.transfer")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg transfer <玩家>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        GuildMember target = guild.getSortedMembers().stream()
                .filter(member -> member.getName().equalsIgnoreCase(args[1]))
                .findFirst().orElse(null);
        if (target == null) {
            plugin.getMessages().send(sender, "kick.target-not-member");
            return;
        }
        plugin.getGuildActions().transfer(player, guild, target);
    }

    private void pvp(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuildActions().togglePvp(player, guild);
    }

    private void dissolve(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.dissolve")) {
            return;
        }
        if (args.length > 1) {
            // 指定公会名: 控制台亦可执行
            Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
            if (guild == null) {
                plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
                return;
            }
            plugin.getMessages().send(sender, "admin.guild-dissolved", Placeholders.of().put("guild", guild.getName()));
            plugin.getGuildManager().dissolveGuild(guild);
            return;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getGuiManager().openConfirm(player, plugin.getMessages().str("dissolve.confirm"),
                () -> plugin.getGuildActions().dissolve(player, guild));
    }

    private void confirm(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        plugin.getConfirmManager().confirm(player);
    }

    private void cancel(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        plugin.getConfirmManager().cancel(player.getUniqueId());
        plugin.getChatInputManager().cancel(player.getUniqueId());
        plugin.getMessages().send(sender, "common.cancelled");
    }

    // ---------------------------------------------------------
    // 公会战
    // ---------------------------------------------------------

    /**
     * 设置公会战场地出生点。
     * <p>用法: /zg setLocation mate &lt;场地名&gt; &lt;1|2|3&gt;
     * 1 = 红队出生点, 2 = 蓝队出生点, 3 = 观看点(可选)。</p>
     */
    private void setLocation(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.setLocation")) {
            return;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            usage(sender, "/zg setLocation mate <场地名> <1|2|3>");
            return;
        }
        // 兼容带类型(mate)与不带类型两种写法
        int offset = "mate".equalsIgnoreCase(args[1]) || "season".equalsIgnoreCase(args[1]) ? 2 : 1;
        if (args.length < offset + 2) {
            usage(sender, "/zg setLocation mate <场地名> <1|2|3>");
            return;
        }
        if (offset == 2 && !"mate".equalsIgnoreCase(args[1])) {
            plugin.getMessages().send(sender, "war.only-mate");
            return;
        }
        String arenaName = args[offset];
        String indexRaw = args[offset + 1];
        int index;
        if ("观看点".equals(indexRaw) || "spectate".equalsIgnoreCase(indexRaw)) {
            index = ArenaManager.SPECTATE;
        } else {
            try {
                index = Integer.parseInt(indexRaw);
            } catch (NumberFormatException exception) {
                usage(sender, "/zg setLocation mate <场地名> <1|2|3>");
                return;
            }
        }
        if (index < 1 || index > 3) {
            usage(sender, "/zg setLocation mate <场地名> <1|2|3>");
            return;
        }
        plugin.getArenaManager().setSpawn(arenaName, index, player.getLocation());
        plugin.getMessages().send(sender, "war.arena-set", Placeholders.of()
                .put("arena", arenaName)
                .put("part", switch (index) {
                    case ArenaManager.RED -> "红队出生点";
                    case ArenaManager.BLUE -> "蓝队出生点";
                    default -> "观看点";
                })
                .put("world", player.getWorld().getName())
                .put("x", (int) player.getLocation().getX())
                .put("y", (int) player.getLocation().getY())
                .put("z", (int) player.getLocation().getZ()));
    }

    /** /zg war 子命令。 */
    private void war(CommandSender sender, String[] args) {
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "" -> {
                if (sender instanceof Player player) {
                    plugin.getGuiManager().openGuildWar(player);
                } else {
                    warStatus(sender);
                }
            }
            case "match" -> warMatch(sender);
            case "cancel" -> warCancel(sender);
            case "leave", "quit" -> warLeave(sender);
            case "status", "info" -> warStatus(sender);
            case "list", "arenas" -> warList(sender);
            case "stop" -> warStop(sender, args);
            case "start", "force" -> warForceStart(sender, args);
            case "queue" -> warQueueAdmin(sender, args, true);
            case "unqueue" -> warQueueAdmin(sender, args, false);
            default -> usage(sender, "/zg war <match|cancel|leave|status|list>");
        }
    }

    private void warMatch(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.war")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        if (guild == null || member == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        if (!member.getRole().atLeast(GuildRole.VICE)) {
            plugin.getMessages().send(sender, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
            return;
        }
        plugin.getGuildWarManager().enqueue(guild, sender);
    }

    private void warCancel(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null || !has(sender, "zhmguild.war")) {
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());
        if (guild == null || member == null) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        if (!member.getRole().atLeast(GuildRole.VICE)) {
            plugin.getMessages().send(sender, "common.no-permission-in-guild", Placeholders.of()
                    .putRaw("role", plugin.getConfigManager().roleDisplay(GuildRole.VICE)));
            return;
        }
        plugin.getGuildWarManager().dequeue(guild, sender);
    }

    private void warLeave(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        plugin.getGuildWarManager().leave(player);
    }

    private void warStatus(CommandSender sender) {
        Guild guild = sender instanceof Player player
                ? plugin.getGuildManager().getGuildByPlayer(player.getUniqueId())
                : null;
        if (guild == null && sender instanceof Player) {
            plugin.getMessages().send(sender, "common.not-in-guild");
            return;
        }
        plugin.getMessages().send(sender, "war.status-header");
        if (guild != null) {
            sender.sendMessage(plugin.getMessages().parse("<gray>我的公会: <yellow>" + guild.getName()
                    + "</yellow> | 状态: " + plugin.getGuildWarManager().statusText(guild)
                    + " | 今日剩余: <yellow>" + plugin.getGuildWarManager().remainingToday(guild)
                    + "</yellow></gray>", sender));
        }
        sender.sendMessage(plugin.getMessages().parse("<gray>匹配队列: <yellow>"
                + plugin.getGuildWarManager().queueSize() + "</yellow> 个公会 | 进行中战斗: <yellow>"
                + plugin.getGuildWarManager().getWars().size() + "</yellow> 场</gray>", sender));
        for (GuildWar war : plugin.getGuildWarManager().getWars()) {
            sender.sendMessage(plugin.getMessages().parse("<gray>  #" + war.getId() + " <red>"
                    + war.getRedGuild().getName() + "</red> vs <blue>" + war.getBlueGuild().getName()
                    + "</blue> @ " + war.getArena().getName() + " | " + war.getState()
                    + " | 剩余 " + war.remainingSeconds() + "s | 存活 "
                    + war.aliveCount(GuildWar.Side.RED) + " vs " + war.aliveCount(GuildWar.Side.BLUE)
                    + " | 击杀 " + war.getKills(GuildWar.Side.RED) + " vs " + war.getKills(GuildWar.Side.BLUE)
                    + "</gray>", sender));
        }
    }

    private void warList(CommandSender sender) {
        if (!has(sender, "zhmguild.view")) {
            return;
        }
        plugin.getMessages().send(sender, "war.arena-header");
        if (plugin.getArenaManager().size() == 0) {
            sender.sendMessage(plugin.getMessages().parse("<gray>  暂无场地, 使用 <yellow>/zg setLocation mate <场地名> 1</yellow> 创建</gray>", sender));
            return;
        }
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            String state = arena.isInUse() ? "<red>使用中</red>" : arena.isReady() ? "<green>空闲</green>" : "<yellow>缺少出生点</yellow>";
            sender.sendMessage(plugin.getMessages().parse("<gray>  <yellow>" + arena.getName() + "</yellow> - "
                    + state + (arena.hasSpectate() ? " <gray>+观看点</gray>" : "") + "</gray>", sender));
        }
    }

    private void warStop(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.warStop")) {
            return;
        }
        if (args.length > 2 && "all".equalsIgnoreCase(args[2])) {
            int count = plugin.getGuildWarManager().stopAll(sender);
            if (count > 0) {
                plugin.getMessages().send(sender, "war.stopped-count", Placeholders.of().put("count", count));
            }
            return;
        }
        if (args.length > 2) {
            int id;
            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                usage(sender, "/zg war stop [all|战斗ID]");
                return;
            }
            GuildWar war = plugin.getGuildWarManager().getWar(id);
            if (war == null) {
                plugin.getMessages().send(sender, "war.none-running");
                return;
            }
            plugin.getGuildWarManager().stopWar(war, sender);
            return;
        }
        if (plugin.getGuildWarManager().getWars().isEmpty()) {
            plugin.getMessages().send(sender, "war.none-running");
            return;
        }
        int count = plugin.getGuildWarManager().stopAll(sender);
        plugin.getMessages().send(sender, "war.stopped-count", Placeholders.of().put("count", count));
    }

    /** 管理员强制开战: /zg war start <红队公会> <蓝队公会> [场地名] */
    private void warForceStart(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.warStart")) {
            return;
        }
        if (args.length < 4) {
            usage(sender, "/zg war start <红队公会> <蓝队公会> [场地名]");
            return;
        }
        Guild red = plugin.getGuildManager().getGuildByName(args[2]);
        Guild blue = plugin.getGuildManager().getGuildByName(args[3]);
        if (red == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[2]));
            return;
        }
        if (blue == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[3]));
            return;
        }
        Arena arena = args.length > 4 ? plugin.getArenaManager().get(args[4]) : plugin.getArenaManager().findAvailable();
        if (arena == null) {
            plugin.getMessages().send(sender, "war.no-arena");
            return;
        }
        if (plugin.getGuildWarManager().startWar(red, blue, arena, sender) != null) {
            plugin.getMessages().send(sender, "war.force-started", Placeholders.of()
                    .put("red", red.getName())
                    .put("blue", blue.getName())
                    .put("arena", arena.getName()));
        }
    }

    /** 管理员代替公会入队 / 出队: /zg war queue|unqueue <公会名> */
    private void warQueueAdmin(CommandSender sender, String[] args, boolean enqueue) {
        if (!has(sender, "zhmguild.warStart")) {
            return;
        }
        if (args.length < 3) {
            usage(sender, "/zg war " + (enqueue ? "queue" : "unqueue") + " <公会名>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByName(args[2]);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[2]));
            return;
        }
        if (enqueue) {
            if (plugin.getGuildWarManager().enqueue(guild, sender, true)) {
                plugin.getMessages().send(sender, "war.force-queued", Placeholders.of()
                        .put("guild", guild.getName())
                        .put("count", plugin.getGuildWarManager().queueSize()));
            }
        } else {
            if (plugin.getGuildWarManager().dequeue(guild, sender)) {
                plugin.getMessages().send(sender, "war.force-unqueued", Placeholders.of().put("guild", guild.getName()));
            }
        }
    }

    /** 自检: GUI 配置 / 运行状态。 */
    private void debug(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.admin")) {
            return;
        }
        sender.sendMessage(plugin.getMessages().parse("<gray>===== <yellow>ZHMguild 自检</yellow> =====</gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>存储: <yellow>"
                + (plugin.getDatabase().isMysql() ? "MySQL" : "SQLite")
                + "</yellow> | 公会: <yellow>" + plugin.getGuildManager().getGuildCount()
                + "</yellow> | 成员索引: <yellow>" + plugin.getGuildManager().getGuilds().stream()
                .mapToInt(Guild::getMemberCount).sum() + "</yellow></gray>", sender));
        sender.sendMessage(plugin.getMessages().parse("<gray>经济: <yellow>"
                + (plugin.getEconomyHook().isEnabled() ? "Vault" : "未启用")
                + "</yellow> | 点券: <yellow>" + (plugin.getPointsHook().isEnabled() ? "PlayerPoints" : "未启用")
                + "</yellow> | 变量: <yellow>" + (plugin.getPlaceholderHook() != null ? "PlaceholderAPI" : "未启用")
                + "</yellow></gray>", sender));

        List<String> errors = plugin.getGuiConfig().validate();
        if (errors.isEmpty()) {
            sender.sendMessage(plugin.getMessages().parse("<green>GUI 配置检查通过, 全部菜单按钮均可正常构建.</green>", sender));
        } else {
            sender.sendMessage(plugin.getMessages().parse("<red>GUI 配置存在 " + errors.size() + " 个问题:</red>", sender));
            for (String error : errors) {
                sender.sendMessage(plugin.getMessages().parse("<red>  - <gray>" + error + "</gray></red>", sender));
            }
        }
    }

    // ---------------------------------------------------------
    // 管理员命令
    // ---------------------------------------------------------

    private void view(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.view")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg view <guild|player|list> [名称]");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "guild" -> {
                if (args.length < 3) {
                    usage(sender, "/zg view guild <公会名>");
                    return;
                }
                Guild guild = plugin.getGuildManager().getGuildByName(args[2]);
                if (guild == null) {
                    plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[2]));
                    return;
                }
                sendGuildInfo(sender, guild);
            }
            case "player" -> {
                if (args.length < 3) {
                    usage(sender, "/zg view player <玩家名>");
                    return;
                }
                GuildMember member = plugin.getGuildManager().getMemberByName(args[2]);
                if (member == null) {
                    plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", args[2]));
                    return;
                }
                Guild guild = plugin.getGuildManager().getGuildByPlayer(member.getUuid());
                plugin.getMessages().send(sender, "admin.status", Placeholders.of()
                        .put("status", (guild == null ? "无公会" : guild.getName())
                                + " | 职位: " + plugin.getConfigManager().roleDisplay(member.getRole())
                                + " | 贡献: " + TimeUtil.money(member.getContribution())));
            }
            case "list" -> {
                plugin.getMessages().send(sender, "gui.guild-list-title", Placeholders.of()
                        .put("sort", "全部")
                        .put("total", plugin.getGuildManager().getGuildCount()));
                for (Guild guild : plugin.getGuildManager().sortedGuilds(SortType.LEVEL)) {
                    sender.sendMessage(plugin.getMessages().parse("<yellow>" + guild.getName()
                            + "</yellow> <dark_gray>|</dark_gray> <gray>会长 " + guild.getLeaderName()
                            + " | 等级 " + guild.getLevel() + " | 成员 " + guild.getMemberCount() + "</gray>", sender));
                }
            }
            default -> usage(sender, "/zg view <guild|player|list> [名称]");
        }
    }

    private void modify(CommandSender sender, String[] args, String mode) {
        if (!has(sender, "zhmguild." + mode)) {
            return;
        }
        if (args.length < 4) {
            usage(sender, "/zg " + mode + " <guildMoney|guildActive|guildOre|player> <名称> <数量>");
            return;
        }
        Double amount = number(sender, args[3]);
        if (amount == null) {
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        String target = args[2].replace("${player}", sender.getName());
        // 语言文件使用 add / take / set 三种键名
        String key = "give".equals(mode) ? "add" : mode;
        Guild guild;
        if ("player".equals(type)) {
            GuildMember member = plugin.getGuildManager().getMemberByName(target);
            if (member == null) {
                plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", target));
                return;
            }
            double value = switch (mode) {
                case "give" -> member.getContribution() + amount;
                case "take" -> Math.max(0, member.getContribution() - amount);
                default -> amount;
            };
            member.setContribution(value);
            guild = plugin.getGuildManager().getGuildByPlayer(member.getUuid());
            if (guild != null) {
                plugin.getGuildManager().saveMember(guild, member);
            }
            plugin.getMessages().send(sender, "player." + key, Placeholders.of()
                    .put("player", member.getName())
                    .put("amount", TimeUtil.money(amount)));
            return;
        }
        guild = plugin.getGuildManager().getGuildByName(target.replace("${guildName}", target));
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", target));
            return;
        }
        switch (type) {
            case "guildmoney" -> {
                double value = switch (mode) {
                    case "give" -> guild.getFunds() + amount;
                    case "take" -> Math.max(0, guild.getFunds() - amount);
                    default -> amount;
                };
                guild.setFunds(value);
                plugin.getGuildManager().saveGuild(guild);
                plugin.getMessages().send(sender, "money." + key, Placeholders.of()
                        .put("guild", guild.getName())
                        .put("amount", TimeUtil.money(amount)));
            }
            case "guildactive" -> {
                int delta = amount.intValue();
                int value = switch (mode) {
                    case "give" -> guild.getActive() + delta;
                    case "take" -> Math.max(0, guild.getActive() - delta);
                    default -> delta;
                };
                guild.setActive(value);
                plugin.getGuildManager().saveGuild(guild);
                plugin.getMessages().send(sender, "active." + key, Placeholders.of()
                        .put("guild", guild.getName())
                        .put("amount", delta));
            }
            case "guildore" -> {
                int delta = amount.intValue();
                int value = switch (mode) {
                    case "give" -> guild.getOre() + delta;
                    case "take" -> Math.max(0, guild.getOre() - delta);
                    default -> delta;
                };
                guild.setOre(value);
                plugin.getGuildManager().saveGuild(guild);
                plugin.getMessages().send(sender, "ore." + key, Placeholders.of()
                        .put("guild", guild.getName())
                        .put("amount", delta));
            }
            default -> usage(sender, "/zg " + mode + " <guildMoney|guildActive|guildOre|player> <名称> <数量>");
        }
    }

    private void adminCreate(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.adminCreate")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg adminCreate <公会名> [玩家名]");
            return;
        }
        java.util.UUID uuid;
        String playerName;
        if (args.length > 2) {
            Player online = Bukkit.getPlayerExact(args[2]);
            if (online != null) {
                uuid = online.getUniqueId();
                playerName = online.getName();
            } else {
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[2]);
                if (offline == null) {
                    plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", args[2]));
                    return;
                }
                uuid = offline.getUniqueId();
                playerName = offline.getName() == null ? args[2] : offline.getName();
            }
        } else if (sender instanceof Player player) {
            uuid = player.getUniqueId();
            playerName = player.getName();
        } else {
            plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", "控制台"));
            return;
        }
        // 管理员创建不消耗费用
        Guild guild = plugin.getGuildManager().createGuild(uuid, playerName, args[1], null, sender);
        if (guild != null) {
            plugin.getMessages().send(sender, "admin.guild-created", Placeholders.of()
                    .put("player", playerName)
                    .put("guild", guild.getName()));
        }
    }

    private void adminEditName(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.adminEditGuildName")) {
            return;
        }
        if (args.length < 3) {
            usage(sender, "/zg adminEditGuildName <旧公会名> <新公会名>");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
            return;
        }
        if (!plugin.getGuildManager().isValidName(args[2], sender)) {
            return;
        }
        String newName = plugin.getGuildManager().plainName(args[2]);
        Guild existing = plugin.getGuildManager().getGuildByName(newName);
        if (existing != null && existing.getId() != guild.getId()) {
            plugin.getMessages().send(sender, "common.guild-name-taken", Placeholders.of().put("guild", newName));
            return;
        }
        String old = guild.getName();
        guild.setName(newName);
        plugin.getGuildManager().renameIndex(guild, old);
        plugin.getGuildManager().saveGuild(guild);
        plugin.getMessages().send(sender, "admin.name-edited", Placeholders.of()
                .put("old", old)
                .put("new", newName));
    }

    private void setRole(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.setRole")) {
            return;
        }
        if (args.length < 3) {
            usage(sender, "/zg setRole <玩家名> <LEADER|VICE|ELDER|MEMBER>");
            return;
        }
        GuildMember member = plugin.getGuildManager().getMemberByName(args[1]);
        if (member == null) {
            plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", args[1]));
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(member.getUuid());
        if (guild == null) {
            plugin.getMessages().send(sender, "common.target-not-in-guild", Placeholders.of().put("player", member.getName()));
            return;
        }
        GuildRole role = GuildRole.parse(args[2], null);
        if (role == null) {
            usage(sender, "/zg setRole <玩家名> <LEADER|VICE|ELDER|MEMBER>");
            return;
        }
        plugin.getGuildActions().setRole(sender, guild, member, role);
    }

    private void adminUp(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.adminUp")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg adminUp <公会名> [等级]");
            return;
        }
        Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
        if (guild == null) {
            plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
            return;
        }
        if (args.length > 2) {
            Double level = number(sender, args[2]);
            if (level == null) {
                return;
            }
            guild.setLevel(level.intValue());
        } else {
            guild.setLevel(guild.getLevel() + 1);
        }
        plugin.getGuildManager().saveGuild(guild);
        plugin.getMessages().send(sender, "admin.level-set", Placeholders.of()
                .put("guild", guild.getName())
                .put("level", guild.getLevel()));
    }

    private void refresh(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.refresh")) {
            return;
        }
        if (args.length > 1) {
            Guild guild = plugin.getGuildManager().getGuildByName(args[1]);
            if (guild == null) {
                plugin.getMessages().send(sender, "common.guild-not-found", Placeholders.of().put("guild", args[1]));
                return;
            }
            plugin.getGuildManager().refresh(guild);
            plugin.getMessages().send(sender, "admin.refreshed", Placeholders.of().put("guild", guild.getName()));
            return;
        }
        plugin.getGuildManager().refreshAll();
        plugin.getMessages().send(sender, "admin.refreshed", Placeholders.of().put("guild", "全部"));
    }

    private void clear(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.clear")) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/zg clear <application|guild>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "application", "applications" -> {
                int count = 0;
                for (Guild guild : new ArrayList<>(plugin.getGuildManager().getGuilds())) {
                    for (GuildApplication application : new ArrayList<>(guild.getApplications())) {
                        plugin.getGuildManager().removeApplication(guild, application.getUuid());
                        count++;
                    }
                }
                plugin.getMessages().send(sender, "admin.cleared", Placeholders.of()
                        .put("type", "入会申请")
                        .put("count", count));
            }
            case "guild", "guilds" -> {
                int count = plugin.getGuildManager().getGuildCount();
                for (Guild guild : new ArrayList<>(plugin.getGuildManager().getGuilds())) {
                    plugin.getGuildManager().dissolveGuild(guild);
                }
                plugin.getMessages().send(sender, "admin.cleared", Placeholders.of()
                        .put("type", "公会")
                        .put("count", count));
            }
            default -> usage(sender, "/zg clear <application|guild>");
        }
    }

    private void reward(CommandSender sender, String[] args) {
        if (!has(sender, "zhmguild.reward")) {
            return;
        }
        if (args.length < 4) {
            usage(sender, "/zg reward <money|points|active|contribution> <玩家名> <数量>");
            return;
        }
        Double amount = number(sender, args[3]);
        if (amount == null) {
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            plugin.getMessages().send(sender, "common.player-not-found", Placeholders.of().put("player", args[2]));
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        boolean success = switch (type) {
            case "money" -> plugin.getEconomyHook().isEnabled() && plugin.getEconomyHook().deposit(target, amount);
            case "points" -> plugin.getPointsHook().isEnabled() && plugin.getPointsHook().give(target.getUniqueId(), amount.intValue());
            case "active" -> {
                Guild guild = plugin.getGuildManager().getGuildByPlayer(target.getUniqueId());
                if (guild != null) {
                    plugin.getGuildManager().addActive(guild, amount.intValue());
                }
                yield guild != null;
            }
            case "contribution" -> {
                Guild guild = plugin.getGuildManager().getGuildByPlayer(target.getUniqueId());
                GuildMember member = plugin.getGuildManager().getMember(target.getUniqueId());
                if (guild != null && member != null) {
                    member.addContribution(amount);
                    plugin.getGuildManager().saveMember(guild, member);
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        if (!success) {
            usage(sender, "/zg reward <money|points|active|contribution> <玩家名> <数量>");
            return;
        }
        plugin.getMessages().send(sender, "admin.rewarded", Placeholders.of()
                .put("player", target.getName())
                .put("amount", TimeUtil.money(amount))
                .put("type", type));
        plugin.getMessages().send(target, "admin.rewarded-target", Placeholders.of()
                .put("amount", TimeUtil.money(amount))
                .put("type", type));
    }

    // ---------------------------------------------------------
    // Tab 补全
    // ---------------------------------------------------------

    private static final List<String> SUB_COMMANDS = List.of(
            "help", "open", "me", "create", "join", "accept", "deny", "invitation", "leave", "up",
            "spawn", "sethome", "signin", "contribute", "top", "members", "info", "notice",
            "kick", "promote", "demote", "transfer", "pvp", "dissolve", "confirm", "cancel", "war");

    private static final List<String> ADMIN_COMMANDS = List.of(
            "reload", "view", "give", "take", "set", "adminCreate", "adminEditGuildName", "setRole",
            "adminUp", "refresh", "clear", "reward", "debug", "setLocation");
    private static final List<String> WAR_ACTIONS = List.of("match", "cancel", "leave", "status", "list");

    private static final List<String> MODIFY_TYPES = List.of("guildMoney", "guildActive", "guildOre", "player");
    private static final List<String> SORTS = List.of("LEVEL", "ACTIVE", "MONTH_ACTIVE", "FUNDS", "MEMBERS", "CREATE_TIME");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            SUB_COMMANDS.stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
            if (sender.hasPermission("zhmguild.admin")) {
                ADMIN_COMMANDS.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
            }
            return result;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open", "top" -> {
                if (args.length == 2) {
                    SORTS.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            case "join", "info", "members", "dissolve", "refresh", "adminup" -> {
                if (args.length == 2) {
                    plugin.getGuildManager().getGuilds().stream()
                            .map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                            .forEach(result::add);
                }
            }
            case "invitation", "kick", "promote", "demote", "transfer" -> {
                if (args.length == 2) {
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                            .forEach(result::add);
                }
            }
            case "accept", "deny" -> {
                if (args.length == 2) {
                    plugin.getGuildManager().getGuilds().stream()
                            .map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                            .forEach(result::add);
                }
            }
            case "view" -> {
                if (args.length == 2) {
                    List.of("guild", "player", "list").stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                } else if (args.length == 3) {
                    plugin.getGuildManager().getGuilds().stream().map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            case "give", "take", "set" -> {
                if (args.length == 2) {
                    MODIFY_TYPES.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                } else if (args.length == 3) {
                    plugin.getGuildManager().getGuilds().stream().map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            case "setrole" -> {
                if (args.length == 2) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                } else if (args.length == 3) {
                    for (GuildRole role : GuildRole.values()) {
                        if (role.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                            result.add(role.name());
                        }
                    }
                }
            }
            case "admineditguildname", "admincreate" -> {
                if (args.length == 2 || (sub.equals("admincreate") && args.length == 3)) {
                    plugin.getGuildManager().getGuilds().stream().map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            case "clear" -> {
                if (args.length == 2) {
                    List.of("application", "guild").stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                }
            }
            case "war", "guildwar" -> {
                if (args.length == 2) {
                    WAR_ACTIONS.stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                    if (sender.hasPermission("zhmguild.admin")) {
                        List.of("start", "stop", "queue", "unqueue").stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                    }
                } else if (args.length == 3 && ("start".equalsIgnoreCase(args[1]) || "stop".equalsIgnoreCase(args[1])
                        || "queue".equalsIgnoreCase(args[1]) || "unqueue".equalsIgnoreCase(args[1]))) {
                    plugin.getGuildManager().getGuilds().stream().map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                } else if (args.length == 4 && "start".equalsIgnoreCase(args[1])) {
                    plugin.getGuildManager().getGuilds().stream().map(Guild::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                } else if (args.length >= 5 && "start".equalsIgnoreCase(args[1])) {
                    plugin.getArenaManager().names().stream()
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            case "setlocation" -> {
                if (args.length == 2) {
                    List.of("mate").stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                } else if (args.length == 3) {
                    plugin.getArenaManager().names().stream()
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                } else if (args.length == 4) {
                    List.of("1", "2", "3").stream().filter(name -> name.startsWith(prefix)).forEach(result::add);
                }
            }
            case "reward" -> {
                if (args.length == 2) {
                    List.of("money", "points", "active", "contribution").stream()
                            .filter(name -> name.startsWith(prefix)).forEach(result::add);
                } else if (args.length == 3) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(result::add);
                }
            }
            default -> {
                // 无补全
            }
        }
        return result;
    }
}
