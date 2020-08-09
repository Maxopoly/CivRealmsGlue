package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import static com.civrealms.crgpve.PVEMiscellaneousListener.LOG;
import java.util.HashMap;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * @author Crimeo
 */
public class LagMonitoringListener  implements Listener {
    private HashMap<Chunk,Integer> hopperActivityMap = new HashMap<Chunk,Integer>();
    private HashMap<Chunk,Integer> pistonActivityMap = new HashMap<Chunk,Integer>();
    private HashMap<Chunk,Integer> repeaterActivityMap = new HashMap<Chunk,Integer>();
    private HashMap<Chunk,Integer> chunkLoadActivityMap = new HashMap<Chunk,Integer>();
    private HashMap<Chunk,Integer> observerActivityMap = new HashMap<Chunk,Integer>();
    private CivRealmsGlue plugin;
    
    public LagMonitoringListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler
    public void onHopperLagCheck(InventoryMoveItemEvent event){
        Location loc = event.getDestination().getLocation();
        Chunk c = loc.getChunk();
        if (hopperActivityMap.containsKey(c)){
            hopperActivityMap.put(c,hopperActivityMap.get(c) + 1);
            if (hopperActivityMap.get(c) % 10000 == 0){
                LOG.info("[CivRealmsGlue][LAGCHECK] Hopper Activity so far in chunk at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " has reached " + hopperActivityMap.get(c) + " move events.");
            }
        } else {
            hopperActivityMap.put(c,1);
        }
    }
    
    @EventHandler
    public void onPistonLagCheck(BlockPistonExtendEvent event){
        Location loc = event.getBlock().getLocation();
        Chunk c = loc.getChunk();
        if (pistonActivityMap.containsKey(c)){
            pistonActivityMap.put(c,pistonActivityMap.get(c) + 1);
            if (pistonActivityMap.get(c) % 1000 == 0){
                LOG.info("[CivRealmsGlue][LAGCHECK] Piston Activity so far in chunk at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " has reached " + pistonActivityMap.get(c) + " move events.");
            }
        } else {
            pistonActivityMap.put(c,1);
        }
    }
    
    @EventHandler
    public void onRedstoneLagCheck(BlockRedstoneEvent event){
        if (event.getBlock().getType() == Material.DIODE_BLOCK_OFF){
            Location loc = event.getBlock().getLocation();
            Chunk c = loc.getChunk();
            if (repeaterActivityMap.containsKey(c)){
                repeaterActivityMap.put(c,repeaterActivityMap.get(c) + 1);
                if (repeaterActivityMap.get(c) % 5000 == 0){
                    LOG.info("[CivRealmsGlue][LAGCHECK] Repeater Activity so far in chunk at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " has reached " + repeaterActivityMap.get(c) + " events.");
                }
            } else {
                repeaterActivityMap.put(c,1);
            }
        }
    }
    
    @EventHandler
    public void onObserverLagCheck(BlockPhysicsEvent event){
        if (event.getBlock().getType() == Material.OBSERVER){
            Location loc = event.getBlock().getLocation();
            Chunk c = loc.getChunk();
            if (observerActivityMap.containsKey(c)){
                observerActivityMap.put(c,observerActivityMap.get(c) + 1);
                if (observerActivityMap.get(c) % 10000 == 0){
                    LOG.info("[CivRealmsGlue][LAGCHECK] Observer Activity so far in chunk at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " has reached " + observerActivityMap.get(c) + " events.");
                }
            } else {
                observerActivityMap.put(c,1);
            }
        }
    }
    
    @EventHandler
    public void onChunkLoadLagCheck(ChunkLoadEvent event){
        Chunk c = event.getChunk();
        Location loc = c.getBlock(0,0,0).getLocation();
        if (chunkLoadActivityMap.containsKey(c)){
            chunkLoadActivityMap.put(c,chunkLoadActivityMap.get(c) + 1);
            if (chunkLoadActivityMap.get(c) % 50 == 0){
                LOG.info("[CivRealmsGlue][LAGCHECK] Chunk Load Activity so far in chunk at " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " has reached " + chunkLoadActivityMap.get(c) + " events.");
            }
        } else {
            chunkLoadActivityMap.put(c,1);
        }
    }
    
    @EventHandler
    public void onCreatureLagCheck(CreatureSpawnEvent event){
        int nearbyEntityCount = (event.getLocation().getWorld().getNearbyEntities(event.getLocation(), 2, 2, 2)).size();
        if (nearbyEntityCount > 10 && Math.random() < 0.1){
            LOG.info("[CivRealmsGlue][LAGCHECK] Breeding Entity Proximity Alert at " + event.getLocation().getX() + ", " + event.getLocation().getY() + ", " + event.getLocation().getZ());
        }
    }
    
}
