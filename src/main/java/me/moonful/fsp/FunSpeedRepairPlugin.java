package me.moonful.fsp;

import org.bukkit.plugin.java.JavaPlugin;

public class FunSpeedRepairPlugin extends JavaPlugin {

    private static FunSpeedRepairPlugin instance;
    private double multiplier;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(new MendingListener(this), this);
        getCommand("fsp").setExecutor(new FspCommand(this));

        getLogger().info("FunSpeedRepair enabled");
    }

    public void loadConfigValues() {
        this.multiplier = getConfig().getDouble("exp-repair", 0.5);
    }

    public void reloadValues() {
        reloadConfig();
        loadConfigValues();
    }

    public double getMultiplier() {
        return multiplier;
    }

    public static FunSpeedRepairPlugin getInstance() {
        return instance;
    }
}
