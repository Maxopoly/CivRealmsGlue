package com.civrealms.delayedtasks;

/**
 * @author Crimeo
 */
import com.civrealms.crgmain.CivRealmsGlue;
import static com.civrealms.crgmain.CivRealmsGlue.LOG;
import com.civrealms.crgpve.AltAccountListener;
import com.civrealms.crgpve.SekkritClass;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.programmerdan.minecraft.banstick.data.BSPlayer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.ConsoleCommandSender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DelayedAsync {

    public static class SlowPlayerLoopTask extends BukkitRunnable {

        private final CivRealmsGlue plugin;

        public SlowPlayerLoopTask(CivRealmsGlue plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() { //(async)
            plugin.LOG.info("[CivRealmsGlue] loopstart " + System.currentTimeMillis());

            HashSet<String> allPlayers = new HashSet<String>(); //fill up in world loops already used below, will use for VPN check.

            //##### Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tps"); //####### paper doesn't like this
            //kick excess players at high load:
            //kick alts to keep server pop at or below 90 per world:
            for (World w : Bukkit.getServer().getWorlds()) {
                List<Player> playerList = w.getPlayers();
                int extraPlayers = 0;
                if (playerList != null && playerList.size() > plugin.perWorldAltThreshold) {
                    extraPlayers = playerList.size() - plugin.perWorldAltThreshold;
                }
                plugin.LOG.info("player list size " + playerList.size());

                HashSet<Player> eligibleToKick = new HashSet<Player>();
                Set<Integer> families = AltAccountListener.getAltFamiliesInverse().keySet();
                if (families != null) {
                    for (int familyID : families) {
                        Set<UUID> altSet = AltAccountListener.getAltFamiliesInverse().get(familyID);
                        if (altSet != null) {
                            for (UUID uid : altSet) {
                                if (Bukkit.getPlayer(uid) == null || !Bukkit.getPlayer(uid).isOnline()) {
                                    altSet.remove(uid);
                                }
                            }
                        }
                        //only online family members left now:
                        if (altSet != null && altSet.size() > 1) {
                            long earliest = Long.MAX_VALUE;
                            UUID earliestUid = null;
                            for (UUID uid : altSet) {
                                Player p = Bukkit.getPlayer(uid);
                                if (p.getLastPlayed() < earliest) {
                                    earliest = p.getLastPlayed();
                                    earliestUid = uid;
                                }
                            }
                            if (earliestUid != null) {
                                //plugin.LOG.info("earliest uid: " + earliestUid);
                                eligibleToKick.add(Bukkit.getPlayer(earliestUid));
                            }
                        }
                    }
                    //plugin.LOG.info("eligible to kick size: " + eligibleToKick.size());
                    for (int i = 0; i < extraPlayers; i++) {
                        int randIndex = (int) (Math.random() * (double) eligibleToKick.size());
                        int index = 0;
                        if (eligibleToKick != null) {
                            for (Player p : eligibleToKick) {
                                if (index == randIndex) {
                                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                        public void run() {
                                            p.kickPlayer(ChatColor.RED + "Do not immediately relog on." + ChatColor.WHITE + "You were kicked for being the least recently logged on alt of a group of alts during high server load. Please do not rejoin with alts until a later time. The system may begin auto-banning you if this happens too many times in a row quickly.");
                                        }
                                    });
                                    eligibleToKick.remove(p);
                                    plugin.updateSlowLoopKicks(p, 1);
                                    plugin.LOG.info("[CRG] altLag kick, player " + p.getDisplayName());
                                    break;
                                }
                                index++;
                            }
                        }
                    }
                }
            }

            //kick alts of banned/pearled people and sundry other things:
            HashSet<UUID> pearledPlayers = new HashSet<UUID>();
            try {
                this.plugin.openConnection();
                Statement statement = this.plugin.getConnection().createStatement();
                ResultSet result = statement.executeQuery("select uid from exilepearls where freed_offline != 1;");
                while (result.next()) {
                    pearledPlayers.add(UUID.fromString(result.getString(1)));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (World w : Bukkit.getServer().getWorlds()) {
                for (Player p : w.getPlayers()) {
                    if (Math.random() < 0.05 && w.getName().equalsIgnoreCase("new_world")
                            && SekkritClass.checkForFemboyIsland(p.getLocation().getX(), p.getLocation().getZ())
                            && p.getLocation().getBlock().getBiome() != Biome.OCEAN) {
                        p.sendMessage(ChatColor.DARK_GREEN + "You are currently standing on FEMBOY ISLAND.");
                        p.sendMessage(ChatColor.DARK_GREEN + "Please enjoy your stay.");
                    }

                    allPlayers.add(p.getName());

                    //alts
                    BSPlayer bsplayer = BSPlayer.byUUID(p.getUniqueId());
                    if (bsplayer != null) {
                        Set<BSPlayer> alts = bsplayer.getTransitiveSharedPlayers(true);

                        //pearled?
                        if (!w.getName().equalsIgnoreCase("Civrealms_Lobby")) {
                            if (pearledPlayers.contains(p.getUniqueId()) && !w.getName().equals("prison_the_end")) { //(don't kick people from prison world for being in prison lol)
                                if (p.isOnline()) {
                                    //Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                        //public void run() {
                                            //p.kickPlayer("Autokicked, for detected pearl evasion. Modmail Admins on reddit.com/r/civrealms modmail with any questions or if in error.");
                                        //}
                                    //});
                                    pearledPlayers.remove(p.getUniqueId()); // they will get re-added next loop. But we need to account for people freed a few minutes later, etc. so they don't just get banned right after freeing
                                    plugin.updateSlowLoopKicks(p, 1);
                                    plugin.LOG.info("[CivRealmsGlue]: incremented penalty " + p.getDisplayName() + " for pearl evasion.");
                                }
                            }
                            for (BSPlayer alt : alts) {
                                if (pearledPlayers.contains(alt.getUUID()) && !w.getName().equals("prison_the_end") && Bukkit.getPlayer(alt.getUUID()) != null && Bukkit.getPlayer(alt.getUUID()).isOnline()) { //think this was the bug, was checking is main was online thus people with many alts got +1 penalty point no matter what per alt in existence.
                                    //Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                        //public void run() {
                                            //p.kickPlayer("Autokicked, for detected pearl evasion. Modmail Admins on reddit.com/r/civrealms modmail with any questions or if in error.");
                                        //}
                                    //});
                                    pearledPlayers.remove(p.getUniqueId()); // they will get re-added next loop. But we need to account for people freed a few minutes later, etc. so they don't just get banned right after freeing
                                    plugin.updateSlowLoopKicks(p, 1);
                                    plugin.LOG.info("[CivRealmsGlue]: incremented penalty " + p.getDisplayName() + " for pearl evasion.");
                                    break;
                                }
                            }
                        }

                        //banned?
                        boolean banned = false;
                        for (BSPlayer alt : alts) {
                            if (alt.getBan() != null) {
                                banned = true;
                                break;
                            }
                        }
                        if (banned) {
                            if (p.isOnline()) {
                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    public void run() {
                                        p.kickPlayer("Autobanned, for detected ban evasion. Modmail Admins on reddit.com/r/civrealms modmail with any questions or if in error.");
                                    }
                                });
                            }
                            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                            String command = "banstick " + p.getDisplayName() + " Autobanned for detected ban evasion. Modmail Admins on reddit.com/r/civrealms modmail with any questions or if in error.";
                            Bukkit.dispatchCommand(console, command);
                            plugin.LOG.info("[CivRealmsGlue]: looptask kicked and banned a player, " + p.getDisplayName());
                        }
                    } else {
                        try {
                            plugin.LOG.info("[CivRealmsGlue] slowlooptask bsplayer null: " + p.getDisplayName());
                        } catch (NullPointerException e) {
                            plugin.LOG.info("[CivRealmsGlue] slowlooptask bsplayer null: trying to get name also nulled.");
                        }
                    }
                }
            }

            //recheck all VPNs with manual overrides from config, etc. This is still all async no big deal, and query below looks crazy but is one batch query which goes quite fast anyway
            try {
                //list of players online right now and their MOST RECENT connection strings and their LEAST RECENT join dates in UNIX time
                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                String query = "select maxIIDs.name, connection, min_unix_join from "
                        + "(select name, bs_session.pid, iid from "
                        + "(select name, realms.bs_session.pid, max(sid) as maxSid from realms.bs_session "
                        + "left join realms.bs_player on bs_player.pid = bs_session.pid where proxy_pardon_time is null and ("; //this last proxy_pardon_time thing leaves out /forgive proxy people
                Iterator it = allPlayers.iterator();
                while (it.hasNext()) {
                    query = query + "name = '" + it.next() + "' or ";
                }
                query = query + "name = 'placeholderplaceholder') group by 1) maxSids "
                        + "left join realms.bs_session on bs_session.sid = maxSids.maxSid group by 1,2,3) maxIIDs "
                        + "left join realms.bs_ip_data on maxIIDs.iid = bs_ip_data.iid "
                        + "left join ("
                        + "select name, UNIX_TIMESTAMP(join_time) as min_unix_join from "
                        + "(select name, bs_session.pid, join_time from "
                        + "(select name, realms.bs_session.pid, max(sid) as maxSid from realms.bs_session "
                        + "left join realms.bs_player on bs_player.pid = bs_session.pid where proxy_pardon_time is null and ("; //this last proxy_pardon_time thing leaves out /forgive proxy people
                it = allPlayers.iterator();
                while (it.hasNext()) {
                    query = query + "name = '" + it.next() + "' or ";
                }
                query = query + "name = 'placeholderplaceholder') group by 1) maxSids "
                        + "left join realms.bs_session on bs_session.sid = maxSids.maxSid group by 1,2,3) maxIIDs "
                        + "group by 1,2"
                        + ") firstJoins on firstJoins.name = maxIIDs.name "
                        + "group by 1,2,3;";
                //plugin.getLogger().info("DEBUG query is: " + query);
                ResultSet rs = statement.executeQuery(query);

                String playerName = "";
                String connection = "";
                long min_unix_join = 0;
                //plugin.getLogger().info("did big query");
                while (rs.next()) {
                    connection = null;
                    playerName = rs.getString(1);
                    connection = rs.getString(2);
                    Player bukkitPlayer = Bukkit.getPlayer(playerName);
                    if (bukkitPlayer != null && connection != null && !plugin.checkISPAsWL(connection)) {
                        //brand new connection string
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            public void run() {
                                bukkitPlayer.kickPlayer(ChatColor.RED + "You are not in trouble, your ISP is just unfamiliar to us." + ChatColor.WHITE + " You have only been kicked not banned. Either connect with a different ISP right away, or contact us at https://www.reddit.com/message/compose?to=%2Fr%2FCivRealms or https://discord.gg/ez42Ac to whitelist your ISP.");
                            }
                        });
                        plugin.updateSlowLoopKicks(bukkitPlayer, 1); //do want to eventually ban them before they can crawl 3 blocks at a time to someone's house.
                        plugin.LOG.info("[CivRealmsGlue]: looptask kicked a player for new ISP, " + bukkitPlayer.getDisplayName() + " and ISP: " + connection);
                        continue;
                    }
                    min_unix_join = rs.getLong(3) * 1000; //for millis
                    if (!plugin.firstPlayed.containsKey(playerName.toLowerCase())) {
                        plugin.firstPlayed.put(playerName.toLowerCase(), min_unix_join);
                        //plugin.getLogger().info("adding to firstplayed: " + playerName.toLowerCase() + ", " + min_unix_join);
                    }
                    if (connection == null && playerName == null) {
                        continue;
                    } else if (connection == null) {
                        //but playerName doesn't
                        if (bukkitPlayer != null && System.currentTimeMillis() - min_unix_join > plugin.noIPGracePeriod) {
                            plugin.LOG.info("[CivRealmsGlue] plugin thinks this player joined " + (System.currentTimeMillis() - min_unix_join) + " ago.");
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                public void run() {
                                    bukkitPlayer.kickPlayer(ChatColor.RED + "Please follow these steps in order! " + ChatColor.GOLD + "1) " + ChatColor.WHITE + "Switch off your VPN, if any. " + ChatColor.GOLD + " 2) " + ChatColor.WHITE + "Wait 5 full minutes and try logging on again. " + ChatColor.GOLD + "3) " + ChatColor.WHITE + "Ask for help at /r/civrealms 'message the moderators' or on our discord at https://discord.gg/ez42Ac The system is having trouble verifying your IP address as residential.");
                                }
                            });
                            plugin.updateSlowLoopKicks(bukkitPlayer, 4);
                            plugin.LOG.info("[CivRealmsGlue]: looptask kicked a player for X minute IP unresolved, " + bukkitPlayer.getDisplayName());
                        }
                        continue;
                    }

                    if (connection != null && !connection.equals("") && plugin.avgProxies.containsKey(connection) && plugin.avgProxies.get(connection) > 2.3) { //configThreshold){
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) {
                            plugin.LOG.info("[CRG] Slowlooptask VPN section, player is: " + player.getDisplayName() + " and connection is: " + connection + " and proxy is: " + plugin.avgProxies.get(connection));
                            if (plugin.avgProxies.get(connection) < 3.0) { //higher than threshold but less than 100%, maybe VPN maybe not, be more polite
                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    public void run() {
                                        player.kickPlayer(ChatColor.DARK_RED + "Do not log back on on the same IP right away. " + ChatColor.WHITE + "You have been kicked for VPS / VPN Proxy use. Do not log back on immediately, you will just get banned after too many same attempts. Please switch to a residential IP. For questions, modmail the admins at www.reddit.com/r/Civrealms to discuss or to clear you to play if this is in error.");
                                    }
                                });
                                plugin.updateSlowLoopKicks(player, 4);
                                plugin.LOG.info("[CivRealmsGlue]: looptask kicked and banned a player VPN severity 1, " + player.getDisplayName());
                                continue;
                            } else { //3.0 right level so basically 100% VPN
                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    public void run() {
                                        player.kickPlayer(ChatColor.WHITE + "You have been banned for VPS / VPN Proxy use. Your IP is especially high risk, so you were not kicked first. Modmail the admins at www.reddit.com/r/Civrealms to discuss and clear you to play.");
                                    }
                                });
                                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                                String command = "banstick " + player.getDisplayName() + " You have been banned for VPS / VPN Proxy use. Your IP is especially high risk, so you were not kicked first. Modmail the admins at www.reddit.com/r/Civrealms to discuss and clear you to play.";
                                Bukkit.dispatchCommand(console, command);
                                plugin.LOG.info("[CivRealmsGlue]: looptask kicked and banned a player VPN severity 2, " + player.getDisplayName());
                                continue;
                            }
                        }
                        plugin.getLogger().info("[CivRealmsGlue] slowLoopTask kicked a player on connection: " + connection + " player = " + playerName + " for VPN");
                    } else {
                        if (!plugin.avgProxies.containsKey(connection)) {
                            plugin.getLogger().info("[CivRealmsGlue] unknown connection found and not otherwise handled: " + connection + " player = " + playerName);
                        }
                        //but otherwise do nothing, just log.
                    }
                }

                if (!statement.isClosed()) {
                    statement.close();
                };
            } catch (SQLException e) {
                e.printStackTrace();
            }

            plugin.LOG.info("[CivRealmsGlue] loopend " + System.currentTimeMillis());
        }
    }

    public static class BeaconDisable extends BukkitRunnable {

        private final CivRealmsGlue plugin;

        public BeaconDisable(CivRealmsGlue plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() { //(async)
            for (World w : Bukkit.getServer().getWorlds()) {

                //disable beacons in certain worlds where would be OP
                if (plugin.getConfig().getString("worldName").contentEquals("aqua")
                        || plugin.getConfig().getString("worldName").contentEquals("new_world")) {
                    for (Player p : w.getPlayers()) {
                        if (p.hasPotionEffect(PotionEffectType.REGENERATION) && p.getPotionEffect(PotionEffectType.REGENERATION).getAmplifier() == 0) {
                            p.removePotionEffect(PotionEffectType.REGENERATION);
                        }
                        if (p.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
                            p.removePotionEffect(PotionEffectType.FAST_DIGGING);
                        }
                        if (p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                            p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                        }
                    }
                }
            }
        }
    }
    
    public static class SaveFingerPrintDB extends BukkitRunnable {
        
        private final CivRealmsGlue plugin;
        private final Connection connection;
        
        public SaveFingerPrintDB(CivRealmsGlue plugin, Connection connection) {
            this.plugin = plugin;
            this.connection = connection;
        }
        
        @Override
        public void run() {
            try {
                plugin.openConnection();
                Statement statement = connection.createStatement(); //NULL EXCEPTIONS?
                ResultSet result = statement.executeQuery("select * from fingerprints;");
                String writeQuery = "INSERT INTO fingerprints (`uuid`,`fingerprint`) VALUES"; //this doesn't care what name you put on the prints, so no risk of SQL injection
                HashMap<UUID, String> dbKeys = new HashMap<UUID, String>();
                while (result.next()) {
                    String uuid = result.getString(1);
                    String fingerprint = result.getString(2);
                    dbKeys.put(UUID.fromString(uuid), fingerprint);
                }
                Iterator iter = plugin.fingerprintMap.keySet().iterator();
                while (iter.hasNext()) {
                    UUID uuid = (UUID) iter.next();
                    if (!dbKeys.containsKey(uuid)) {
                        writeQuery = writeQuery + "('" + uuid + "','" + plugin.fingerprintMap.get(uuid) + "'),";
                    }
                }

                if (writeQuery.endsWith(",")) {
                    writeQuery = writeQuery.substring(0, writeQuery.length() - 1); //chop off last comma
                } else {
                    if (!statement.isClosed()) {
                        statement.close();
                    };
                    return; //if nothing to save, don't try to run the query
                }
                writeQuery = writeQuery + ";";
                statement.executeUpdate(writeQuery);
                if (!statement.isClosed()) {
                    statement.close();
                };
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static class LogInviteDBTask extends BukkitRunnable {
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
    
    public static class UnInviteDB extends BukkitRunnable { //check in command handler that both players exist and get the UUID.
        
        private final CivRealmsGlue plugin;
        private final Connection connection;
        Player inviter;
        String invitee;
        
        public UnInviteDB(CivRealmsGlue plugin, Connection connection, Player inviter, String invitee) {
            this.plugin = plugin;
            this.connection = connection;
            this.inviter = inviter;
            this.invitee = invitee;
        }
        
        @Override
        public void run() {
            try{
                plugin.openConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate("DELETE FROM ott_invites where display_name_invitee = '" + invitee + "' and display_name_inviter = '" + inviter.getDisplayName() + "';");
                if (!statement.isClosed()){statement.close();}
                inviter.sendMessage(ChatColor.GREEN + "Invitation rescinded.");
            } catch (SQLException e){
                e.printStackTrace();
                inviter.sendMessage(ChatColor.RED + "There was an error in the database, please notify staff of a bug.");
            }
        }
    }
    
    public static class CheckOTTInvite extends BukkitRunnable { // do the checks for player being new and blah blah in the command handler. This is DB checks only.
        
        private final CivRealmsGlue plugin;
        private final Connection connection;
        Player invitee;
        String inviter;
        
        public CheckOTTInvite(CivRealmsGlue plugin, Connection connection, Player invitee, String inviter) {
            this.plugin = plugin;
            this.connection = connection;
            this.inviter = inviter;
            this.invitee = invitee;
        }
        
        @Override
        public void run() {
            try{
                plugin.openConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select * from ott_invites where display_name_invitee = '" + invitee.getDisplayName() + "';");
                while (result.next()) {
                    String display_name_inviter = result.getString(1);
                    String display_name_invitee = result.getString(2);
                    long timestamp = result.getLong(3);
                    int x = result.getInt(4);
                    int y = result.getInt(5);
                    int z = result.getInt(6);
                    String world = result.getString(7);

                    if (display_name_inviter.equalsIgnoreCase(inviter)){ //this entry in DB is the one invitee requested
                        LOG.info("[CivRealmsGlue] ott invite DB log, inviter: " + display_name_inviter + ", invitee: " + display_name_invitee + ", timestamps: " + (System.currentTimeMillis() - timestamp) + " world string: " + world + " invitee world string: " + invitee.getWorld().getName());
                        if (System.currentTimeMillis() - timestamp < 600000){
                            if (world.equalsIgnoreCase(invitee.getWorld().getName())){
                                plugin.setPostOTTInventory(invitee);
                                invitee.teleport(new Location(invitee.getWorld(),x,y,z));
                                plugin.getUsedTheirTeleport().add(invitee.getUniqueId());
                                return;
                            } else {
                                //lots of API calls here, fire off a synch request
                                BukkitRunnable r = new BukkitRunnable() { 
                                    public void run() {
                                        //send over info about where to teleport them etc later when they get there
                                        ByteArrayDataOutput out1 = ByteStreams.newDataOutput();
                                        out1.writeUTF("Forward");
                                        out1.writeUTF(world);
                                        out1.writeUTF("ott");
                                        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
                                        DataOutputStream msgout = new DataOutputStream(msgbytes);
                                        try {
                                            msgout.writeUTF(invitee.getDisplayName());
                                            msgout.writeInt(x);
                                            msgout.writeInt(y);
                                            msgout.writeInt(z);
                                        }
                                        catch (IOException e) {
                                          e.printStackTrace();
                                          invitee.sendMessage(ChatColor.RED + "There was an error in pluginMessaging, please notify staff of a bug.");
                                        } 
                                        out1.writeShort(msgbytes.toByteArray().length);
                                        out1.write(msgbytes.toByteArray());
                                        invitee.sendPluginMessage(plugin, "BungeeCord", out1.toByteArray());

                                        //actually teleport them
                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                        out.writeUTF("Connect");
                                        out.writeUTF(world);
                                        invitee.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                                        invitee.getInventory().clear();
                                        plugin.getCPT().getTagManager().untag(invitee.getUniqueId());
                                    }
                                };
                                r.runTask(plugin);
                                return;
                            }
                        } else {
                            invitee.sendMessage(ChatColor.RED + "That invitation has expired after 10 minutes. Please ask the inviting player to invite you again.");
                            return;
                        }
                    }
                }
                invitee.sendMessage(ChatColor.RED + "No player has invited you. Please arrange an ott ahead of time and have the other person first /ottinvite [your name]");
                return;
            } catch (SQLException e){
                e.printStackTrace();
                invitee.sendMessage(ChatColor.RED + "There was an error in the database, please notify staff of a bug.");
                return;
            }
        }
    }
    
    public static class WhitelistISP extends BukkitRunnable {
        
        private final CivRealmsGlue plugin;
        private final  String connection;
        
        public WhitelistISP(CivRealmsGlue plugin, String connection) {
            this.plugin = plugin;
            this.connection = connection;
        }
        
        @Override
        public void run() {
            try {
                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                statement.executeUpdate("INSERT into isp_whitelist VALUES('" + connection + "') ON DUPLICATE KEY UPDATE connection = connection;");
                plugin.whitelistedConnections.add(connection.toLowerCase());
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static class DustForPrints extends BukkitRunnable { //check for having a kit on hand in the command handler not here
        
        private final CivRealmsGlue plugin;
        private final Player player;
        private final int x;
        private final int y;
        private final int z;
        
        public DustForPrints(CivRealmsGlue plugin, Player player, int x, int y, int z) {
            this.plugin = plugin;
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public void run() {
            try {
                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                ResultSet rs = statement.executeQuery("SELECT * from fingerprints_environmental where x > " + (x-10) 
                        + " and x < " + (x+10) + " and y > " + (y-10) + " and y < " + (y+10) + " and z > " + (z-10) + " and z < " + (z+10) + " and world = '" + player.getWorld().getName() + "';");

                while (rs.next()){
                    String fingerprint = rs.getString(7);
                    ItemStack is = new ItemStack(Material.PAPER, 1);
                    ItemMeta meta = is.getItemMeta();
                    meta.setDisplayName("Fingerprints");
                    String la[] = new String[]{"A sheet of fingerprints", "for an unknown player", fingerprint};
                    List<String> lorearray = Arrays.asList(la);
                    meta.setLore(lorearray);
                    is.setItemMeta(meta);
                    BukkitRunnable r = new BukkitRunnable() {
                        public void run() {
                            player.getWorld().dropItemNaturally(new Location(player.getWorld(),x,y,z), is); //don't put items in inventory, async very stupid/dangerous. Drop them at the inputted coords
                        }
                    };
                    r.runTask(plugin);
                }
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static class SetMainCharacter extends BukkitRunnable {
        
        private final CivRealmsGlue plugin;
        private final Player player;
        
        public SetMainCharacter(CivRealmsGlue plugin, Player player) {
            this.plugin = plugin;
            this.player = player;
        }
        
        @Override
        public void run() {
            try{
                plugin.openConnection();
                BSPlayer bsplayer = BSPlayer.byUUID(player.getUniqueId());
                Set<BSPlayer> alts = bsplayer.getTransitiveSharedPlayers(true);
                Statement statement = plugin.getConnection().createStatement();

                for (BSPlayer alt : alts) {
                    String query = "update player_data set main = '" + player.getUniqueId() + "' where player_name like '%" + 
                        alt.getName()+ "%';";
                    statement.executeUpdate(query);
                }

                player.sendMessage("This is now your main account.");
                if (!statement.isClosed()){statement.close();};
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
    }
    
    public static class ProcessVote extends BukkitRunnable {
        
        private final CivRealmsGlue plugin;
        private final OfflinePlayer player;
        private final long time;
        private final String website;
        
        public ProcessVote(CivRealmsGlue plugin, OfflinePlayer player, long time, String website) {
            this.plugin = plugin;
            this.player = player;
            this.time = time;
            this.website = website;
        }
        
        @Override
        public void run() {
            try {
                double staminaAmount = 0;
                if (website.equals("MinecraftServers.org")){staminaAmount = plugin.MinecraftServersOrg;}
                if (website.equals("PlanetMinecraft.com")){staminaAmount = plugin.PlanetMinecraftCom;}
                if (website.equals("MCSL")){staminaAmount = plugin.MCSL;}

                plugin.openConnection();
                Statement statement = plugin.getConnection().createStatement();
                ResultSet result = statement.executeQuery(
                        "select stamina, vote_millis, player_name from player_data where player_uuid = '"
                        + player.getUniqueId() + "';");

                if (result.next()) {

                    Double prior_stamina = Double.valueOf(result.getDouble(1));
                    long numVotes = result.getLong(2);
                    if (numVotes > 100) {
                        numVotes = 0; //legacy overwrite
                    }

                    //String ipin = result.getString(4);
                    if (numVotes < 3) { //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!REPLACE WHEN ADDING NEW SITES WITH +1 EACH
                        LOG.info("Vote PD block: est was greater than last");
                        Set<BSPlayer> directAssoc = BSPlayer.byUUID(player.getUniqueId()).getTransitiveSharedPlayers(true);
                        String q = "";
                        for (BSPlayer alt : directAssoc) {
                            q = "UPDATE player_data SET vote_millis = " + (numVotes + 1)
                                    + " where player_uuid = '" + alt.getUUID() + "';";
                            statement = plugin.getConnection().createStatement();
                            statement.executeUpdate(q);
                        }
                        statement.close();

                        Statement statement2 = plugin.getConnection().createStatement();
                        LOG.info("PlayerData: " + player.getName() + " uuid " + player.getUniqueId().toString() + " was given stamina for voting, this much: " + Double.toString(staminaAmount) + " and numVotes WAS " + numVotes);
                        prior_stamina = Double.valueOf(prior_stamina.doubleValue() + staminaAmount);
                        q = "UPDATE player_data SET stamina = " + prior_stamina + " where player_uuid = '"
                                + player.getUniqueId() + "';";
                        statement2.executeUpdate(q);
                        statement2.close();
                    }

                    if (player instanceof Player) {
                        Player splayer = (Player) player;
                        splayer.sendMessage(String.valueOf(splayer.getCustomName()) + " has " + prior_stamina + " Stamina");
                    }
                    plugin.getLogger().info(String.valueOf(player.getName()) + " has " + prior_stamina + " Stamina");
                } else {

                    if (player instanceof Player) {
                        Player splayer = (Player) player;
                        splayer.sendMessage("Not in DB");
                    }
                    plugin.getLogger().info("Not in DB");
                }
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
