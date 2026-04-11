package MoonFul;

import org.bukkit.plugin.java.JavaPlugin;

public class FunSpeedRepair extends JavaPlugin {

    private static FunSpeedRepair instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new MendingListener(), this);
    }

    public static FunSpeedRepair getInstance() {
        return instance;
    }
}