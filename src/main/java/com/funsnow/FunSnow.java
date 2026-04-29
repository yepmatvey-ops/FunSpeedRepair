package com.funsnow;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FunSnow extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Set<UUID> frozenJump = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("funsnowball").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ================= ITEM =================
    private ItemStack item(int amount) {
        ItemStack i = new ItemStack(Material.SNOWBALL, amount);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§b❄ FunSnow");
        i.setItemMeta(m);
        return i;
    }

    // ================= WG =================
    private boolean isBlocked(Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        for (ProtectedRegion r : set) {
            if (getConfig().getStringList("blocked-region").contains(r.getId())) {
                return true;
            }
        }
        return false;
    }

    // ================= COMMANDS =================
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {

        if (!(s instanceof Player)) return true;
        Player p = (Player) s;

        if (a.length == 0) return true;

        if (a[0].equalsIgnoreCase("give")) {

            if (!p.hasPermission("funsnow.admin") && !p.isOp()) return true;

            Player t = Bukkit.getPlayer(a[1]);
            int amt = Integer.parseInt(a[2]);

            if (t != null) {
                t.getInventory().addItem(item(amt));
            }
        }

        if (a[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("funsnow.admin") && !p.isOp()) return true;
            reloadConfig();
        }

        return true;
    }

    // ================= COOLDOWN =================
    @EventHandler
    public void throwBall(ProjectileLaunchEvent e) {

        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Player p = (Player) e.getEntity().getShooter();

        long cd = getConfig().getLong("cooldown") * 1000;

        if (cooldown.containsKey(p.getUniqueId())) {
            long passed = System.currentTimeMillis() - cooldown.get(p.getUniqueId());

            if (passed < cd) {
                e.setCancelled(true);
                return;
            }
        }

        cooldown.put(p.getUniqueId(), System.currentTimeMillis());
    }

    // ================= HIT =================
    @EventHandler
    public void hit(ProjectileHitEvent e) {

        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Location hit = e.getEntity().getLocation();
        double radius = getConfig().getDouble("radius");

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                if (ticks > 40) { // ~2 sec particles
                    cancel();
                    return;
                }

                // ================= PARTICLES =================

                for (double x = -radius; x <= radius; x += 1) {
                    for (double y = 0; y <= 2; y += 1) {
                        for (double z = -radius; z <= radius; z += 1) {

                            Location loc = hit.clone().add(x, y, z);

                            if (loc.distance(hit) > radius) continue;

                            // WHITE SNOW CUBE
                            hit.getWorld().spawnParticle(
                                    Particle.SNOW_SHOVEL,
                                    loc,
                                    1,
                                    0, 0, 0,
                                    0
                            );

                            // BLUE VORTEX (center)
                            if (loc.distance(hit) < radius / 2) {
                                hit.getWorld().spawnParticle(
                                        Particle.REDSTONE,
                                        loc,
                                        1,
                                        new Particle.DustOptions(Color.BLUE, 1)
                                );
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);

        // ================= EFFECTS =================
        for (Player p : hit.getWorld().getPlayers()) {

            double dist = p.getLocation().distance(hit);
            if (dist > radius) continue;

            if (isBlocked(p.getLocation())) continue;

            String[] slow = getConfig().getString("effects.SLOW").split(":");

            int base = Integer.parseInt(slow[0]) * 20;
            int amp = Integer.parseInt(slow[1]);

            double f = 1 - (dist / radius);
            int duration = (int) (base * f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amp));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0));

            frozenJump.add(p.getUniqueId());
        }
    }

    // ================= JUMP BLOCK =================
    @EventHandler
    public void move(PlayerMoveEvent e) {

        Player p = e.getPlayer();

        if (!frozenJump.contains(p.getUniqueId())) return;

        if (e.getTo().getY() > e.getFrom().getY()) {
            p.teleport(e.getFrom());
        }
    }
}
