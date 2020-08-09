/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.civrealms.crgmain;

import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Crimeo
 */
class LogInviteDBTask extends BukkitRunnable {
        Player inviter;
        String invitee;
        String worldName;
        CivRealmsGlue plugin;

        public LogInviteDBTask(CivRealmsGlue plugin, String worldName, Player inviter, String invitee){
            this.worldName = worldName;
            this.inviter = inviter;
            this.invitee = invitee;
            this.plugin = plugin;
        }
        
        @Override
        public void run() {
            try {
                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                statement.executeUpdate("INSERT INTO ott_invites VALUES('" + inviter.getDisplayName() + "', '" + invitee + "', "
                        + System.currentTimeMillis() + ", " + inviter.getLocation().getBlockX() + ", " + inviter.getLocation().getBlockY() + ", " + inviter.getLocation().getBlockZ() + ", '"
                        + worldName + "') ON DUPLICATE KEY UPDATE timestamp = "
                        + System.currentTimeMillis() + ", x = " + inviter.getLocation().getBlockX() + ", y = " + inviter.getLocation().getBlockY() + ", z = " + inviter.getLocation().getBlockZ()
                        + ", world = '" + worldName + "';");
                inviter.sendMessage(ChatColor.GREEN + "You have invited " + invitee + " (ensure that spelling is correct) to ott. They will NOT be notified, please use /tell. ");
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                inviter.sendMessage(ChatColor.RED + "There was an error in the database, please notify staff of a bug.");
            }
        }
    }
