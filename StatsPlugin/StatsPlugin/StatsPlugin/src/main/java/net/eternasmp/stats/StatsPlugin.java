package net.eternasmp.stats;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsPlugin extends JavaPlugin {

    private static StatsPlugin instance;
    private Economy economy;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault (with an economy plugin, e.g. ExcellentEconomy) not found! Disabling EternaStats.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.statsManager = new StatsManager(this);

        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("statsadmin").setExecutor(new StatsAdminCommand(this));

        getServer().getPluginManager().registerEvents(new StatsListener(this), this);

        // Re-apply attributes for anyone already online (e.g. /reload)
        getServer().getOnlinePlayers().forEach(p -> statsManager.applyAllStats(p));

        getLogger().info("EternaStats enabled — " + StatType.values().length + " stats loaded.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.saveAll();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public static StatsPlugin getInstance() {
        return instance;
    }
}
