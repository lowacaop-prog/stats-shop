package net.eternasmp.stats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsAdminCommand implements CommandExecutor {

    private final StatsPlugin plugin;

    public StatsAdminCommand(StatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eternastats.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /statsadmin <reset|set|reload> <player> [stat] [level]", NamedTextColor.GRAY));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getStatsManager().loadConfigValues();
            sender.sendMessage(Component.text("EternaStats config reloaded.", NamedTextColor.LIGHT_PURPLE));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /statsadmin <reset|set> <player> [stat] [level]", NamedTextColor.GRAY));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!(target.isOnline())) {
            sender.sendMessage(Component.text("That player must be online for this command.", NamedTextColor.RED));
            return true;
        }
        Player onlineTarget = (Player) target;

        switch (sub) {
            case "reset" -> {
                PlayerStatData data = plugin.getStatsManager().getData(onlineTarget);
                for (StatType type : StatType.values()) {
                    data.setLevel(type, 0);
                    if (type.isToggleable()) {
                        data.setActiveLevel(type, 0);
                    }
                }
                plugin.getStatsManager().saveAll();
                plugin.getStatsManager().applyAllStats(onlineTarget);
                sender.sendMessage(Component.text("Reset all stats for " + onlineTarget.getName() + ".", NamedTextColor.LIGHT_PURPLE));
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /statsadmin set <player> <stat> <level>", NamedTextColor.GRAY));
                    return true;
                }
                StatType type;
                try {
                    type = StatType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Unknown stat: " + args[2], NamedTextColor.RED));
                    return true;
                }
                int level;
                try {
                    level = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Level must be a number.", NamedTextColor.RED));
                    return true;
                }
                level = Math.max(0, Math.min(level, plugin.getStatsManager().getMaxLevel()));
                PlayerStatData data = plugin.getStatsManager().getData(onlineTarget);
                data.setLevel(type, level);
                if (type.isToggleable()) {
                    data.setActiveLevel(type, level);
                }
                plugin.getStatsManager().saveAll();
                plugin.getStatsManager().applyStat(onlineTarget, type);
                sender.sendMessage(Component.text("Set " + type.getDisplayName() + " to level " + level + " for " + onlineTarget.getName() + ".", NamedTextColor.LIGHT_PURPLE));
            }
            default -> sender.sendMessage(Component.text("Usage: /statsadmin <reset|set|reload> <player> [stat] [level]", NamedTextColor.GRAY));
        }

        return true;
    }
}
