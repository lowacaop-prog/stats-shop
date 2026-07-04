package net.eternasmp.stats;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;

public enum StatType {

    ATTACK_SPEED(Attribute.GENERIC_ATTACK_SPEED, Material.SUGAR, "Attack Speed", "%", true, false),
    KNOCKBACK_RESISTANCE(Attribute.GENERIC_KNOCKBACK_RESISTANCE, Material.SHIELD, "Knockback Resistance", "%", true, false),
    ATTACK_DAMAGE(Attribute.GENERIC_ATTACK_DAMAGE, Material.IRON_SWORD, "Attack Damage", "", false, false),
    LUCK(Attribute.GENERIC_LUCK, Material.EMERALD, "Luck", "", false, false),
    MOVEMENT_SPEED(Attribute.GENERIC_MOVEMENT_SPEED, Material.FEATHER, "Movement Speed", "%", true, true),
    // Flying Speed has no backing Attribute (null) — player flight speed is controlled via
    // Player#setFlySpeed(), handled specially in StatsManager, not through the attribute system.
    FLYING_SPEED(null, Material.ELYTRA, "Flying Speed", "%", true, true),
    MAX_HEALTH(Attribute.GENERIC_MAX_HEALTH, Material.GOLDEN_APPLE, "Max Health", " HP", false, false);

    private final Attribute attribute;
    private final Material icon;
    private final String displayName;
    private final String suffix;
    private final boolean percentageBased;
    private final boolean toggleable;

    StatType(Attribute attribute, Material icon, String displayName, String suffix, boolean percentageBased, boolean toggleable) {
        this.attribute = attribute;
        this.icon = icon;
        this.displayName = displayName;
        this.suffix = suffix;
        this.percentageBased = percentageBased;
        this.toggleable = toggleable;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSuffix() {
        return suffix;
    }

    /**
     * If true, the per-level effect value is treated as a percentage modifier
     * (MULTIPLY_SCALAR_1). If false, it's a flat additive modifier (ADD_NUMBER).
     */
    public boolean isPercentageBased() {
        return percentageBased;
    }

    /** If true, this stat supports the "unlock permanently, activate freely at a lower level" mechanic. */
    public boolean isToggleable() {
        return toggleable;
    }
}
