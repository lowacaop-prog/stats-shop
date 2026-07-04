package net.eternasmp.stats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

public class StatsListener implements Listener {

    private final StatsPlugin plugin;

    public StatsListener(StatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Slight delay so attributes are fully initialized after entity load
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                plugin.getStatsManager().applyAllStats(player), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getStatsManager().unload(event.getPlayer());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        // EssentialsX (and vanilla) reset flySpeed to their own default when /fly toggles.
        // Re-apply our bonus a tick later so it isn't immediately overwritten.
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                plugin.getStatsManager().applyFlyingSpeed(player), 1L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof StatsGUI.MainHolder) {
            event.setCancelled(true);
            handleMainClick(event);
        } else if (event.getInventory().getHolder() instanceof StatsGUI.ToggleHolder holder) {
            event.setCancelled(true);
            handleToggleClick(event, holder.type);
        }
    }

    private void handleMainClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        StatType type = StatsGUI.slotToStat(event.getSlot());
        if (type == null) return;

        if (type.isToggleable() && event.isShiftClick()) {
            StatsGUI.openToggleMenu(player, plugin, type);
            return;
        }

        StatsManager.PurchaseResult result = plugin.getStatsManager().purchase(player, type);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(Component.text("Upgraded " + type.getDisplayName() + "!", NamedTextColor.LIGHT_PURPLE));
                StatsGUI.openMain(player, plugin);
            }
            case MAX_LEVEL -> player.sendMessage(Component.text("That stat is already at max level.", NamedTextColor.GOLD));
            case INSUFFICIENT_FUNDS -> player.sendMessage(Component.text("You don't have enough money for that upgrade.", NamedTextColor.RED));
        }
    }

    private void handleToggleClick(InventoryClickEvent event, StatType type) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int maxLevel = plugin.getStatsManager().getMaxLevel();
        int level = StatsGUI.slotToLevel(event.getSlot(), maxLevel);
        if (level < 0) return;
        int unlocked = plugin.getStatsManager().getData(player).getLevel(type);
        if (level > unlocked) return;

        plugin.getStatsManager().setActiveLevel(player, type, level);
        player.sendMessage(Component.text(type.getDisplayName() + " active level set to " + level + ".", NamedTextColor.LIGHT_PURPLE));
        StatsGUI.openToggleMenu(player, plugin, type);
    }
}
