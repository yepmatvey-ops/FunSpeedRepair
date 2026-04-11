package me.moonful.fsp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemMendEvent;

public class MendingListener implements Listener {

    private final FunSpeedRepairPlugin plugin;

    public MendingListener(FunSpeedRepairPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {

        double m = plugin.getMultiplier();

        int original = event.getRepairAmount();
        int modified = (int) Math.max(1, Math.round(original * m));

        event.setRepairAmount(modified);
    }
}
