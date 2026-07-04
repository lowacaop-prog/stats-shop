package net.eternasmp.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final StatsPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, PlayerStatData> cache = new HashMap<>();

    private double costBase;
    private double costMultiplier;
    private int maxLevel;
    private final Map<StatType, Double> perLevelEffect = new HashMap<>();

    public StatsManager(StatsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadConfigValues();
        loadDataFile();
    }

    /** Public so /statsadmin reload can actually refresh cached cost/effect values, not just the raw file. */
    public void loadConfigValues() {
        plugin.reloadConfig();
        var cfg = plugin.getConfig();
        costBase = cfg.getDouble("cost.base", 1_600_000);
        costMultiplier = cfg.getDouble("cost.multiplier", 1.6);
        maxLevel = cfg.getInt("cost.max-level", 10);

        for (StatType type : StatType.values()) {
            double val = cfg.getDouble("effects." + type.name() + ".per-level", 0.0);
            perLevelEffect.put(type, val);
        }
    }

    private void loadDataFile() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data.yml", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    // ---------- Cost calculation ----------

    /** Cost to purchase the given level (1-indexed). Level 1 is the first purchase. */
    public long getCostForLevel(int level) {
        if (level < 1) return 0;
        return Math.round(costBase * Math.pow(costMultiplier, level - 1));
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getPerLevelEffect(StatType type) {
        return perLevelEffect.getOrDefault(type, 0.0);
    }

    // ---------- Player data ----------

    public PlayerStatData getData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerStatData data = new PlayerStatData(uuid);
            String path = uuid.toString();
            if (dataConfig.isConfigurationSection(path)) {
                for (StatType type : StatType.values()) {
                    int lvl = dataConfig.getInt(path + "." + type.name(), 0);
                    data.setLevel(type, lvl);
                }
                for (StatType type : StatType.values()) {
                    if (!type.isToggleable()) continue;
                    int active = dataConfig.getInt(path + "." + type.name() + "_active", data.getLevel(type));
                    data.setActiveLevel(type, active);
                }
            }
            return data;
        });
    }

    public void unload(Player player) {
        save(player.getUniqueId());
        cache.remove(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerStatData data = cache.get(uuid);
        if (data == null) return;
        String path = uuid.toString();
        for (StatType type : StatType.values()) {
            dataConfig.set(path + "." + type.name(), data.getLevel(type));
            if (type.isToggleable()) {
                dataConfig.set(path + "." + type.name() + "_active", data.getActiveLevel(type));
            }
        }
        saveToDisk();
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            PlayerStatData data = cache.get(uuid);
            String path = uuid.toString();
            for (StatType type : StatType.values()) {
                dataConfig.set(path + "." + type.name(), data.getLevel(type));
                if (type.isToggleable()) {
                    dataConfig.set(path + "." + type.name() + "_active", data.getActiveLevel(type));
                }
            }
        }
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    // ---------- Purchasing ----------

    public enum PurchaseResult {
        SUCCESS, MAX_LEVEL, INSUFFICIENT_FUNDS
    }

    public PurchaseResult purchase(Player player, StatType type) {
        PlayerStatData data = getData(player);
        int current = data.getLevel(type);
        if (current >= maxLevel) {
            return PurchaseResult.MAX_LEVEL;
        }
        long cost = getCostForLevel(current + 1);
        if (!plugin.getEconomy().has(player, cost)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
        plugin.getEconomy().withdrawPlayer(player, cost);
        data.setLevel(type, current + 1);
        if (type.isToggleable()) {
            // Newly purchased level becomes active automatically
            data.setActiveLevel(type, current + 1);
        }
        save(player.getUniqueId());
        applyStat(player, type);
        if (type == StatType.MAX_HEALTH) {
            // Immediately fill the newly gained heart(s) rather than leaving them empty
            AttributeInstance healthInstance = player.getAttribute(StatType.MAX_HEALTH.getAttribute());
            if (healthInstance != null) {
                double added = getPerLevelEffect(StatType.MAX_HEALTH);
                player.setHealth((float) Math.min(healthInstance.getValue(), player.getHealth() + added));
            }
        }
        return PurchaseResult.SUCCESS;
    }

    /** Toggle the active level for a toggleable stat (does not cost money, capped at unlocked level). */
    public void setActiveLevel(Player player, StatType type, int level) {
        PlayerStatData data = getData(player);
        data.setActiveLevel(type, level);
        save(player.getUniqueId());
        applyStat(player, type);
    }

    // ---------- Attribute application ----------

    public void applyAllStats(Player player) {
        for (StatType type : StatType.values()) {
            applyStat(player, type);
        }
    }

    /** Vanilla default fly speed (matches Player#getFlySpeed() default). */
    private static final float BASE_FLY_SPEED = 0.1f;

    public void applyStat(Player player, StatType type) {
        if (type == StatType.FLYING_SPEED) {
            applyFlyingSpeed(player);
            return;
        }

        AttributeInstance instance = player.getAttribute(type.getAttribute());
        if (instance == null) return;

        NamespacedKey key = new NamespacedKey(plugin, type.name().toLowerCase());
        // Remove any existing modifier we previously applied
        instance.getModifiers().stream()
                .filter(m -> m.getKey().equals(key))
                .toList()
                .forEach(instance::removeModifier);

        int effectiveLevel = type.isToggleable()
                ? getData(player).getActiveLevel(type)
                : getData(player).getLevel(type);

        if (effectiveLevel <= 0) {
            return;
        }

        double perLevel = getPerLevelEffect(type);
        double totalBonus = perLevel * effectiveLevel;

        AttributeModifier.Operation op = type.isPercentageBased()
                ? AttributeModifier.Operation.MULTIPLY_SCALAR_1
                : AttributeModifier.Operation.ADD_NUMBER;

        AttributeModifier modifier = new AttributeModifier(key, totalBonus, op);
        instance.addModifier(modifier);
    }

    /**
     * Flying Speed doesn't go through the Attribute system for players — it's controlled by
     * Player#setFlySpeed(), the same field EssentialsX's /fly manipulates. We set it directly
     * here, and this must be re-called whenever flight is toggled (see StatsListener), since
     * Essentials/vanilla often resets flySpeed to its own default when /fly is toggled on.
     */
    public void applyFlyingSpeed(Player player) {
        int level = getData(player).getActiveLevel(StatType.FLYING_SPEED);
        double perLevel = getPerLevelEffect(StatType.FLYING_SPEED);
        float speed = (float) (BASE_FLY_SPEED * (1.0 + perLevel * level));
        speed = Math.max(-1f, Math.min(1f, speed));
        player.setFlySpeed(speed);
    }
}
