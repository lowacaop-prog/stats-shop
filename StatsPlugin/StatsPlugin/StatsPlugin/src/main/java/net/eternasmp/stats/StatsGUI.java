package net.eternasmp.stats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StatsGUI {

    public static final int SIZE = 27;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static class MainHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class ToggleHolder implements InventoryHolder {
        public final StatType type;

        public ToggleHolder(StatType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Centers `count` items in a 9-wide row starting at `rowBaseSlot` (0, 9, or 18). */
    private static int rowStart(int rowBaseSlot, int count) {
        int pad = Math.max(0, (9 - count) / 2);
        return rowBaseSlot + pad;
    }

    /** Returns a slot for each stat, centered in the middle row (row2: 9-17). */
    private static int[] mainMenuSlots() {
        int count = StatType.values().length;
        int start = rowStart(9, count);
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) slots[i] = start + i;
        return slots;
    }

    /**
     * Returns a slot for each level 0..maxLevel, split across row2 and row3,
     * centered independently in each row. Fixed positions regardless of player progress.
     */
    private static int[] levelSlots(int maxLevel) {
        int total = maxLevel + 1;
        int firstRow = (int) Math.ceil(total / 2.0);
        int secondRow = total - firstRow;
        int[] slots = new int[total];
        int idx = 0;
        int start1 = rowStart(9, firstRow);
        for (int i = 0; i < firstRow; i++) slots[idx++] = start1 + i;
        int start2 = rowStart(18, secondRow);
        for (int i = 0; i < secondRow; i++) slots[idx++] = start2 + i;
        return slots;
    }

    public static void openMain(Player player, StatsPlugin plugin) {
        StatsManager mgr = plugin.getStatsManager();
        PlayerStatData data = mgr.getData(player);

        Component title = MM.deserialize(plugin.getConfig().getString("gui.title", "<gradient:#6a11cb:#b06ab3>Stat Upgrades</gradient>"));
        Inventory inv = Bukkit.createInventory(new MainHolder(), SIZE, title);

        ItemStack filler = filler();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        StatType[] types = StatType.values();
        int[] slots = mainMenuSlots();
        for (int i = 0; i < types.length; i++) {
            inv.setItem(slots[i], buildStatItem(mgr, data, types[i]));
        }

        player.openInventory(inv);
    }

    public static void openToggleMenu(Player player, StatsPlugin plugin, StatType type) {
        StatsManager mgr = plugin.getStatsManager();
        PlayerStatData data = mgr.getData(player);
        int unlocked = data.getLevel(type);
        int active = data.getActiveLevel(type);
        int maxLevel = mgr.getMaxLevel();

        Component title = MM.deserialize("<gradient:#6a11cb:#b06ab3>" + type.getDisplayName() + " - Select Level</gradient>");
        Inventory inv = Bukkit.createInventory(new ToggleHolder(type), SIZE, title);

        ItemStack filler = filler();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        int[] slots = levelSlots(maxLevel);
        for (int lvl = 0; lvl <= maxLevel; lvl++) {
            boolean isUnlocked = lvl <= unlocked;
            ItemStack item;
            ItemMeta meta;
            List<Component> lore = new ArrayList<>();

            if (!isUnlocked) {
                item = new ItemStack(Material.GRAY_DYE);
                meta = item.getItemMeta();
                meta.displayName(Component.text("Level " + lvl, NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Locked", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Purchase this level in the main menu", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                double bonus = mgr.getPerLevelEffect(type) * lvl * 100;
                item = new ItemStack(lvl == active ? Material.LIME_DYE : type.getIcon());
                meta = item.getItemMeta();
                meta.displayName(Component.text("Level " + lvl, lvl == active ? NamedTextColor.GREEN : NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("+" + trim(bonus) + "% " + type.getDisplayName().toLowerCase(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                if (lvl == active) {
                    lore.add(Component.text("Currently active", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("Click to activate", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[lvl], item);
        }

        player.openInventory(inv);
    }

    /** Maps an inventory slot back to a level for the toggle menu, or -1 if not a level slot. */
    public static int slotToLevel(int slot, int maxLevel) {
        int[] slots = levelSlots(maxLevel);
        for (int lvl = 0; lvl < slots.length; lvl++) {
            if (slots[lvl] == slot) return lvl;
        }
        return -1;
    }

    /** Maps an inventory slot back to a StatType for the main menu, or null. */
    public static StatType slotToStat(int slot) {
        StatType[] types = StatType.values();
        int[] slots = mainMenuSlots();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) return types[i];
        }
        return null;
    }

    private static ItemStack buildStatItem(StatsManager mgr, PlayerStatData data, StatType type) {
        int level = data.getLevel(type);
        int max = mgr.getMaxLevel();
        boolean isMax = level >= max;

        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<#b06ab3><bold>" + type.getDisplayName() + "</bold></#b06ab3>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level: " + level + " / " + max, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        double perLevel = mgr.getPerLevelEffect(type);
        double currentBonus = perLevel * level * (type.isPercentageBased() ? 100 : 1);
        lore.add(Component.text("Current bonus: +" + trim(currentBonus) + type.getSuffix(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());

        if (isMax) {
            lore.add(Component.text("MAX LEVEL REACHED", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            long cost = mgr.getCostForLevel(level + 1);
            double nextBonusTotal = perLevel * (level + 1) * (type.isPercentageBased() ? 100 : 1);
            lore.add(Component.text("Next level: +" + trim(nextBonusTotal) + type.getSuffix() + " total", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cost: $" + format(cost), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to purchase", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (type.isToggleable() && level > 0) {
            lore.add(Component.text("Shift-click to select active level", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        return pane;
    }

    private static String trim(double val) {
        if (val == Math.floor(val)) {
            return String.valueOf((long) val);
        }
        return String.format("%.1f", val);
    }

    private static String format(long val) {
        return String.format("%,d", val);
    }
}
