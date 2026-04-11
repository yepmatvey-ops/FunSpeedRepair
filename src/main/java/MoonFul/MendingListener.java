package MoonFul;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemMendEvent;

public class MendingListener implements Listener {

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        double multiplier = FunSpeedRepair.getInstance().getConfig().getDouble("exp-repair");

        int original = event.getRepairAmount();
        int reduced = (int) Math.max(0, original * multiplier);

        event.setRepairAmount(reduced);
    }
}