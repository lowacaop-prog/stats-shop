package net.eternasmp.stats;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatData {

    private final UUID uuid;
    private final Map<StatType, Integer> levels = new EnumMap<>(StatType.class);

    // For toggleable stats (Movement Speed, Flying Speed): players unlock up to a level,
    // but can freely choose a lower "active" level at no cost. Defaults to fully unlocked.
    private final Map<StatType, Integer> activeLevels = new EnumMap<>(StatType.class);

    public PlayerStatData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel(StatType type) {
        return levels.getOrDefault(type, 0);
    }

    public void setLevel(StatType type, int level) {
        levels.put(type, level);
        if (type.isToggleable() && getActiveLevel(type) > level) {
            activeLevels.put(type, level);
        }
    }

    public int getActiveLevel(StatType type) {
        return activeLevels.getOrDefault(type, getLevel(type));
    }

    public void setActiveLevel(StatType type, int level) {
        int unlocked = getLevel(type);
        activeLevels.put(type, Math.max(0, Math.min(level, unlocked)));
    }

    public Map<StatType, Integer> getAllLevels() {
        return levels;
    }
}
