package com.civrealms.delayedtasks;

import com.civrealms.crgmain.CivRealmsGlue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Crimeo
 */
public class DelayedSync {
    
    public static class AntiBoatSinkCheck extends BukkitRunnable {

        private final CivRealmsGlue plugin;
        private HashMap<UUID, Location> boatLocs = new HashMap<UUID, Location>();

        public AntiBoatSinkCheck(CivRealmsGlue plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            for (World w : Bukkit.getServer().getWorlds()) {
                //check for boat sinking
                ArrayList<Entity> entities = plugin.getEntities(w);

                for (Entity e : entities) {
                    if (e instanceof Boat) {
                        boatAltitude(e);
                    }
                }
            }
        }
        
        private void boatAltitude(Entity e) { // second argument is for optional rider of vehicle
            UUID uid = e.getUniqueId();
            if (!boatLocs.containsKey(uid)) {
                boatLocs.put(uid, e.getLocation());
            } else {
                Location oldLoc = boatLocs.get(uid);
                Location newLoc = e.getLocation();
                if (newLoc.getY() - oldLoc.getY() < 0) { //moving down
                    Material thisBlock = newLoc.getBlock().getType();
                    Material above = newLoc.getBlock().getRelative(0, 1, 0).getType();
                    Material twoAbove = newLoc.getBlock().getRelative(0, 2, 0).getType();
                    if ((thisBlock == Material.WATER || thisBlock == Material.STATIONARY_WATER)
                            && (above == Material.WATER || above == Material.STATIONARY_WATER)
                            && (twoAbove == Material.WATER || twoAbove == Material.STATIONARY_WATER)) {
                        Block topOfWater = newLoc.getBlock();
                        while (topOfWater.getType() != Material.AIR && topOfWater.getY() < 255) {
                            topOfWater = topOfWater.getRelative(0, 1, 0);
                        }
                        //okay now remove passengers, send boat up, teleport passengers above it and let them get in themselves.
                        //trying to automatically set them back in again does seizurey rubber band bullshit no matter what I try.
                        if (((Boat) e).getPassengers().size() < 2) {
                            for (Entity passenger : ((Boat) e).getPassengers()) {
                                ((Boat) e).removePassenger(passenger);
                                passenger.teleport(topOfWater.getLocation().add(0, 3, 0));
                                if (passenger instanceof LivingEntity) {
                                    plugin.getLogger().info("lev");
                                    ((LivingEntity) passenger).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 50, 253));
                                }
                                BukkitRunnable r = new BukkitRunnable() {
                                    public void run() {
                                        plugin.getLogger().info("enter");
                                        ((Boat) e).addPassenger(passenger);
                                    }
                                };
                                r.runTaskLater(plugin, 5);
                            }
                            e.teleport(topOfWater.getLocation().add(0, 1, 0));
                        }
                    }
                }
            }
        }
    }
}
