package cn.zhm.guild.hook;

import cn.zhm.guild.ZHMguildPlugin;
import cn.zhm.guild.model.Guild;
import cn.zhm.guild.model.GuildMember;
import cn.zhm.guild.model.SortType;
import cn.zhm.guild.util.Placeholders;
import cn.zhm.guild.util.Text;
import cn.zhm.guild.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * PlaceholderAPI 变量扩展。变量前缀: %zhmguild_xxx%
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final ZHMguildPlugin plugin;

    public PlaceholderHook(ZHMguildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zhmguild";
    }

    @Override
    public String getAuthor() {
        return "ZHM";
    }

    @Override
    public String getVersion() {
        try {
            return plugin.getPluginMeta().getVersion();
        } catch (Throwable throwable) {
            return "1.0.0";
        }
    }

    @Override
    public boolean persist() {
        return true;
    }

    /** 把外部变量(例如 PAPI)套用到文本上, 供语言文件使用。 */
    public String apply(CommandSender sender, String text) {
        if (text == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            if (sender instanceof OfflinePlayer offlinePlayer) {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, text);
            }
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, text);
        } catch (Throwable throwable) {
            return text;
        }
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }
        String key = params.toLowerCase();

        // 排行榜变量: top_<type>_<index>
        if (key.startsWith("top_")) {
            return handleTop(key.substring(4));
        }

        if (player == null) {
            return "";
        }
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        GuildMember member = plugin.getGuildManager().getMember(player.getUniqueId());

        switch (key) {
            case "has_guild":
                return guild == null ? "false" : "true";
            case "name", "guild", "guild_name":
                return guild == null ? "" : Text.legacy(guild.getName());
            case "level":
                return guild == null ? "0" : String.valueOf(guild.getLevel());
            case "active":
                return guild == null ? "0" : String.valueOf(guild.getActive());
            case "month_active":
                return guild == null ? "0" : String.valueOf(guild.getMonthActive());
            case "funds", "money":
                return guild == null ? "0" : TimeUtil.money(guild.getFunds());
            case "ore":
                return guild == null ? "0" : String.valueOf(guild.getOre());
            case "members", "member_count":
                return guild == null ? "0" : String.valueOf(guild.getMemberCount());
            case "max_members":
                return guild == null ? "0" : String.valueOf(plugin.getGuildManager().maxMembers(guild));
            case "leader":
                return guild == null ? "" : guild.getLeaderName();
            case "notice":
                return guild == null ? "" : Text.legacy(guild.getNotice());
            case "create_time":
                return guild == null ? "" : TimeUtil.formatDateTime(guild.getCreateTime());
            case "rank":
                return guild == null ? "0" : String.valueOf(plugin.getGuildManager().rank(guild, SortType.LEVEL));
            case "role":
                return member == null ? "" : Text.legacy(plugin.getConfigManager().roleDisplay(member.getRole()));
            case "contribution":
                return member == null ? "0" : TimeUtil.money(member.getContribution());
            case "sign_streak":
                return member == null ? "0" : String.valueOf(member.getSignStreak());
            case "last_signin":
                return member == null ? "" : TimeUtil.formatDate(member.getLastSignIn());
            default:
                return "";
        }
    }

    private String handleTop(String rest) {
        // 形如 level_1 / active_1
        int underscore = rest.lastIndexOf('_');
        if (underscore <= 0) {
            return "";
        }
        String typeName = rest.substring(0, underscore);
        int index;
        try {
            index = Integer.parseInt(rest.substring(underscore + 1));
        } catch (NumberFormatException exception) {
            return "";
        }
        if (index < 1) {
            return "";
        }
        SortType type = SortType.parse(typeName, null);
        if (type == null) {
            return "";
        }
        List<Guild> sorted = plugin.getGuildManager().sortedGuilds(type);
        if (index > sorted.size()) {
            return "";
        }
        return Text.legacy(sorted.get(index - 1).getName());
    }
}
