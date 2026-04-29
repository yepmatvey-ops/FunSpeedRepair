
package com.funsnow;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
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

    // ITEM
    private ItemStack item(int amount){
        ItemStack i = new ItemStack(Material.SNOWBALL, amount);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.AQUA + "❄ FunSnow");
        i.setItemMeta(m);
        return i;
    }

    // WG PRIORITY LOGIC
    private ProtectedRegion getTopRegion(Location loc){
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        ProtectedRegion top = null;
        for(ProtectedRegion r : set){
            if(top == null || r.getPriority() > top.getPriority()){
                top = r;
            }
        }
        return top;
    }

    private boolean isBlocked(Location loc){
        ProtectedRegion r = getTopRegion(loc);
        if(r == null) return false;
        return getConfig().getStringList("blocked-region").contains(r.getId());
    }

    // COMMANDS
    public boolean onCommand(CommandSender s, Command c, String l, String[] a){
        if(!(s instanceof Player)) return true;
        Player p = (Player)s;

        if(a.length==0) return true;

        if(a[0].equalsIgnoreCase("admin") && a.length>=4 && a[1].equalsIgnoreCase("give")){
            if(!p.hasPermission("funsnow.admin")) return true;
            Player t = Bukkit.getPlayer(a[2]);
            int amt = Integer.parseInt(a[3]);
            if(t!=null) t.getInventory().addItem(item(amt));
        }

        if(a[0].equalsIgnoreCase("reload")){
            if(!p.hasPermission("funsnow.admin")) return true;
            reloadConfig();
        }
        return true;
    }

    // COOLDOWN
    @EventHandler
    public void throwBall(ProjectileLaunchEvent e){
        if(!(e.getEntity().getShooter() instanceof Player)) return;
        Player p = (Player)e.getEntity().getShooter();

        long cd = getConfig().getLong("cooldown")*1000;
        if(cooldown.containsKey(p.getUniqueId()) &&
                System.currentTimeMillis()-cooldown.get(p.getUniqueId())<cd){
            e.setCancelled(true);
            return;
        }
        cooldown.put(p.getUniqueId(), System.currentTimeMillis());
    }

    // HIT
    @EventHandler
    public void hit(ProjectileHitEvent e){
        if(!(e.getEntity().getShooter() instanceof Player)) return;

        Location hit = e.getEntity().getLocation();
        double radius = getConfig().getDouble("radius");

        for(Player p : hit.getWorld().getPlayers()){
            double dist = p.getLocation().distance(hit);
            if(dist>radius) continue;

            if(isBlocked(p.getLocation())){
                p.sendMessage(ChatColor.RED + "Blocked region");
                continue;
            }

            double f = 1-(dist/radius);
            int t = (int)(getConfig().getInt("effects.SLOW").split(":")[0].equals("3")?60*f:40*f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, t, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, t, 0));

            frozenJump.add(p.getUniqueId());
        }
    }

    // FREEZE IMPROVED
    @EventHandler
    public void move(PlayerMoveEvent e){
        Player p = e.getPlayer();
        if(!frozenJump.contains(p.getUniqueId())) return;

        if(e.getTo().getY()>e.getFrom().getY()){
            Location l = p.getLocation();
            l.setY(e.getFrom().getY());
            p.teleport(l);
        }
    }
}
