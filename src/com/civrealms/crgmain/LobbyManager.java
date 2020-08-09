package com.civrealms.crgmain;

import com.devotedmc.ExilePearl.event.PlayerPearledEvent;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * @author Crimeo
 */
public class LobbyManager implements Listener{
    
	private CivRealmsGlue plugin;
    
    public LobbyManager(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    //### Some things don't fit here, but a list of where to look for lobby related stuff:
        // 1) in main class: on plugin message received from command, spawn them here in main building, in adventure mode, emptyhanded
        // 2) in restart scripts in the shell/box: daily reset of map back to original
        // 3) in commands class, when using to/from lobby command, randomspawn or back to where you were, or to lobby (but it uses the hashmaps here)
    
    @EventHandler
    public void onCreatureLobby(CreatureSpawnEvent event){
        if (event.getLocation().getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")){
            if(event.getEntity().getType() != EntityType.HORSE){
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    public void onPunchKit(PlayerInteractEvent event){
        if (event.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")){
            Location loc = event.getPlayer().getLocation();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.LAPIS_BLOCK
                    && event.getHand() == EquipmentSlot.HAND){
                    //&& inSafeZone(loc)){
                Block clicked = event.getClickedBlock();
                //click lapis block in spawn zone
                if(clicked.getRelative(0,1,0).getType() == Material.CHEST
                        || clicked.getRelative(0,1,0).getType() == Material.TRAPPED_CHEST){
                    Inventory chestInv = ((Chest)(clicked.getRelative(0,1,0).getState())).getBlockInventory();
                    PlayerInventory pi = event.getPlayer().getInventory();
                    for (ItemStack is : chestInv.getContents()){
                        if(pi.firstEmpty() > -1){
                            pi.setItem(pi.firstEmpty(), is);
                        }
                    }
                    event.getPlayer().sendMessage(ChatColor.AQUA + "Kit received. Overflow items ignored if not enough empty slots.");
                }
            }
        }
    }
    
    @EventHandler
    public void onSafeZoneBreak(BlockBreakEvent event){
        if (inSafeZone(event.getBlock().getLocation()) && !event.getPlayer().isOp()){
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Safe zone is off limits for breaking/building.");
        }
    }
    
    @EventHandler
    public void onSafeZonePlace(BlockPlaceEvent event){
        if (inSafeZone(event.getBlock().getLocation()) && !event.getPlayer().isOp()){
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Safe zone is off limits for breaking/building.");
        }
    }
    
    @EventHandler
    public void onSafeZoneDamage(EntityDamageEvent event){
        if (inSafeZone(event.getEntity().getLocation()) && event.getEntity() instanceof Player){
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onSafeZoneBucketEmpty(PlayerBucketEmptyEvent event){
        if (inSafeZone(event.getPlayer().getLocation()) && event.getPlayer() instanceof Player){
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onSafeZoneBucketFill(PlayerBucketFillEvent event){
        if (inSafeZone(event.getPlayer().getLocation()) && event.getPlayer() instanceof Player){
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLobbyRespawn(PlayerRespawnEvent event){
        if (event.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")){
            event.setRespawnLocation(spawnPlayer(event.getPlayer()));
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLobbySpawn(PlayerJoinEvent event){
        if (!event.getPlayer().isOp()) {
            if (event.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")) {
                spawnPlayer(event.getPlayer());

                event.getPlayer().setGameMode(GameMode.SURVIVAL);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHackySuicide(PlayerCommandPreprocessEvent event){
        if (event.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")
                && event.getMessage().toLowerCase().startsWith("/suicide")){
            event.getPlayer().damage(800);
        }
    }
    
    private Location spawnPlayer(Player p){
        boolean valid = false;
        int spawnX = 0;
        int spawnZ = 0;
        while (!valid){
            spawnX = (int)(Math.random()*7)+plugin.lobbyCenterX-3;
            spawnZ = (int)(Math.random()*7)+plugin.lobbyCenterZ-3;
            valid = true;
            if (plugin.lobbyCenterX - spawnX > -2 && plugin.lobbyCenterX - spawnX < 2 && plugin.lobbyCenterZ - spawnZ > -2 && plugin.lobbyCenterZ - spawnZ < 2){
                valid = false;
            }
        }
        plugin.LOG.info("spawnx " + spawnX + " spawnz " + spawnZ);
        Location toSpawn = new Location(p.getWorld(), spawnX + 0.5, plugin.lobbyCenterY, spawnZ + 0.5);
        p.teleport(toSpawn);
        return toSpawn;
    }
    
    @EventHandler
    public void onPearlLobby(PlayerPearledEvent event){
        if (plugin.getConfig().getString("worldName").equalsIgnoreCase("Civrealms_Lobby")) {
            event.setCancelled(true);
        }
    }
    
    private boolean inSafeZone(Location loc){
        if (loc.getWorld().getName().equalsIgnoreCase("Civrealms_Lobby")){
            if (loc.getZ() > plugin.lobbyNorthSafeBoundary && loc.getZ() < plugin.lobbySouthSafeBoundary
                        && loc.getX() > plugin.lobbyWestSafeBoundary && loc.getX() < plugin.lobbyEastSafeBoundary){
                return true;
            }
        }
        return false;
    }
}
