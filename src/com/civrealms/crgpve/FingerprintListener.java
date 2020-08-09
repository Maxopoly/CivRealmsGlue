/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import isaac.bastion.BastionBlock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class FingerprintListener implements Listener {
    private CivRealmsGlue plugin;
    private HashMap<Location,int[]> pendingInventories = new HashMap<Location,int[]>();
    
    public FingerprintListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
	public void onFingerPrintBreak(BlockBreakEvent event) {
        boolean isReinforced = com.civrealms.crgmain.CitadelChecks.isReinforced(event.getBlock(),event.getPlayer());
        if (isReinforced && Math.random() < plugin.shieldedFingerprintChance){
            LeaveFingerprint lfp = new LeaveFingerprint(plugin.getConfig().getString("worldName"),event.getPlayer(),event.getBlock().getX(),event.getBlock().getY(),event.getBlock().getZ());
            lfp.runTaskAsynchronously(plugin);
        } else if (Math.random() < plugin.normalFingerprintChance){
            LeaveFingerprint lfp = new LeaveFingerprint(plugin.getConfig().getString("worldName"),event.getPlayer(),event.getBlock().getX(),event.getBlock().getY(),event.getBlock().getZ());
            lfp.runTaskAsynchronously(plugin);
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
	public void onFingerPrintPlace(BlockPlaceEvent event) {
        Set<BastionBlock> blocking = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock());
        if (blocking.size() != 0){// && bgm.canPlaceBlock(event.getPlayer(), blocking)){
            for (BastionBlock bb : blocking){
                if (bb.isMature() && Math.random() < plugin.shieldedFingerprintChance){
                    Location loc = bb.getLocation(); //note, location of the bastion block not where they placed.
                    LeaveFingerprint lfp = new LeaveFingerprint(plugin.getConfig().getString("worldName"),event.getPlayer(),loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
                    lfp.runTaskAsynchronously(plugin);
                    return;
                }
            }
        }
        if (Math.random() < plugin.normalFingerprintChance){
            LeaveFingerprint lfp = new LeaveFingerprint(plugin.getConfig().getString("worldName"),event.getPlayer(),event.getBlock().getX(),event.getBlock().getY(),event.getBlock().getZ());
            lfp.runTaskAsynchronously(plugin);
        }
    }
    
    //public void leaveFingerprint(Player player, int x, int y, int z){
    class LeaveFingerprint extends BukkitRunnable {
        int x;
        int y;
        int z;
        Player player;
        String worldName;

        public LeaveFingerprint(String worldName, Player player, int x, int y, int z){
            this.worldName = worldName;
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        @Override
        public void run(){
            try {
                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                ResultSet rs = statement.executeQuery("SELECT fingerprint from fingerprints where uuid = '" + player.getUniqueId().toString() + "';");
                String fingerprint = null;
                if (rs.next()){
                    fingerprint = rs.getString(1);
                } else {
                    fingerprint = plugin.getFingerPrintCode(player.getUniqueId());
                }
                
                statement.executeUpdate("INSERT INTO fingerprints_environmental (x,y,z,world,timestamp,fingerprint) "
                        + "SELECT " + x + ", " + y + ", " + z + ", '" + worldName
                        + "', " + System.currentTimeMillis() + ", '" + fingerprint + "';");
                        //+ ", (select fingerprint from fingerprints where uuid = '" + player.getUniqueId() + "');");
                //uuid(of fingerprint impression),x,y,z,world,timestamp,fingerprint
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
