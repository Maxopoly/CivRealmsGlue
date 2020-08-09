package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

public class SekkritClass implements Listener {
    private static CivRealmsGlue plugin;
    
    //gutted dummy version for third party builds, behavior may not be exactly as expected
    
    public SekkritClass(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    public static Block getHighestCustomForRespawn(int x, int z, World w){
        for (int y = 255; y > -1; y--){
            Block b = w.getBlockAt(x,y,z);
            Material type = b.getType();
            if (type == Material.AIR){
                continue;
            }
            if(type == Material.WATER || type == Material.STATIONARY_WATER //things that might kill you
                    || type == Material.LAVA || type == Material.STATIONARY_LAVA){
                return null;
            }
            return b;
        }
        return null;
    }
    
    public static void flushSpecialLogs(){
    }
    
    public static void deserializeInventory(Inventory inv, String invString){
    }
    
    public static boolean checkLaurel(Block block){
        return false;
    }
    
    public static boolean checkTea(Block block){
        return false;
    }
    
    public static boolean checkCoffee(Block block){
        return false;
    }

    public static boolean possiblyMaliciousAnvilRename(String itemName){
        return false;
    }
    
    public static boolean possiblyMaliciousUsername(String username){
        return false;
    }
    
    public static boolean checkForFemboyIsland(double x, double z){
        return false;
    }
}