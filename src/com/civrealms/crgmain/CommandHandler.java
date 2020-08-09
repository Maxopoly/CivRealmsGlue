package com.civrealms.crgmain;

import com.civrealms.crgpve.SekkritClass;
import com.civrealms.delayedtasks.DelayedAsync;
import com.civrealms.delayedtasks.DelayedAsync.CheckOTTInvite;
import com.civrealms.delayedtasks.DelayedAsync.DustForPrints;
import com.civrealms.delayedtasks.DelayedAsync.LogInviteDBTask;
import com.civrealms.delayedtasks.DelayedAsync.SetMainCharacter;
import com.civrealms.delayedtasks.DelayedAsync.UnInviteDB;
import com.civrealms.delayedtasks.DelayedAsync.WhitelistISP;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.PlayerInventory;

/**
 * @author Crimeo
 */

public class CommandHandler implements CommandExecutor {

    private CivRealmsGlue plugin;
    public static Logger LOG = Logger.getLogger("CivRealmsGlue");
    private HashMap<UUID,Long> dustCooldown = new HashMap<UUID,Long>();

    public CommandHandler(CivRealmsGlue plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (cmd.getName().equalsIgnoreCase("togglereplant")) {
                if (plugin.getNoAutoReplant().contains(player.getUniqueId())) {
                    plugin.getNoAutoReplant().remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Auto replant turned on.");
                    return true;
                } else {
                    plugin.getNoAutoReplant().add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Auto replant turned off.");
                    return true;
                }
            }
            if (cmd.getName().equalsIgnoreCase("ott")) {
                if (System.currentTimeMillis() - player.getFirstPlayed() > 86400000) {
                    player.sendMessage(ChatColor.RED + "You can only use your one time teleport during your first 24 hours after joining.");
                    return true;
                }
                if (plugin.getUsedTheirTeleport().contains(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "The so-called one time teleport is, not surprisingly, a one-time thing.");
                    return true;
                }
                if (!player.getWorld().getName().contentEquals("world")) {
                    player.sendMessage(ChatColor.RED + "You can only use your one time teleport in the main overworld.");
                    return true;
                }
                if (args.length == 1) {
                    if (SekkritClass.possiblyMaliciousUsername(args[0])){ //since this is going into a database
                        player.sendMessage(ChatColor.RED + "Please use only valid minecraft username characters: A-Z, a-z, 0-9, and _");
                        return true;
                    }
                    new CheckOTTInvite(plugin,plugin.getConnection(),player,args[0]).runTaskAsynchronously(plugin);
                    return true;
                }
                return false;
            } else if (cmd.getName().equalsIgnoreCase("ottinvite")) {
                if (player.getWorld().getName().contentEquals("ocean")){
                    player.sendMessage(ChatColor.RED + "You cannot invite people to the ocean realm.");
                    return true;
                } else if (player.getWorld().getName().contains("prison")){
                    player.sendMessage(ChatColor.RED + "You cannot invite people to the prison realm.");
                    return true;
                } else if (player.getWorld().getName().contains("lobby")){
                    player.sendMessage(ChatColor.RED + "You cannot invite people to the lobby realm.");
                    return true;
                }
                if(args.length == 1){
                    if (SekkritClass.possiblyMaliciousUsername(args[0])){ //since this is going into a database
                        player.sendMessage(ChatColor.RED + "Please use only valid minecraft username characters: A-Z, a-z, 0-9, and _");
                        return true;
                    }
                    new LogInviteDBTask(plugin,plugin.getConfig().getString("worldName"),player,args[0]).runTaskAsynchronously(plugin);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You must specify a player's name, /ottinvite [who]");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("ottuninvite")) {
                if (args.length == 1){
                    new UnInviteDB(plugin, plugin.getConnection(),player, args[0]).runTaskAsynchronously(plugin);
                    return true;
                } else {
                    return false;
                }
            } else if (cmd.getName().equalsIgnoreCase("chisel")) {
                ItemStack pick = player.getInventory().getItemInMainHand();
                ItemMeta im = pick.getItemMeta();
                ArrayList<String> lore = new ArrayList<String>();
                if (pick.getType().equals(Material.IRON_PICKAXE)) {
                    if (pick.getItemMeta().hasLore() && pick.getItemMeta().getLore().size() > 1
                            && pick.getItemMeta().getLore().get(0).equals("An alloy of carbon and iron")) {
                        lore.add("An alloy of carbon and iron");
                    } else {
                        lore.add("Made of wrought iron");
                    }
                } else if (pick.getType().equals(Material.DIAMOND_PICKAXE)) {
                    lore.add("Made from laminated sheets of mithril");
                } else if (pick.getType().equals(Material.GOLD_PICKAXE)) {
                    lore.add("An alloy of tin and copper");
                } else {
                    player.sendMessage("Chisel mode only applies to non-primitive picks.");
                    return false;
                }
                if (pick.getItemMeta().hasLore() && pick.getItemMeta().getLore().size() > 1
                        && pick.getItemMeta().getLore().get(1).equals("Mining Mode (/chisel to toggle)")) {
                    lore.add("Chisel Mode (/chisel to toggle)");
                    player.sendMessage("Chisel mode set.");
                } else if (pick.getItemMeta().hasLore() && pick.getItemMeta().getLore().size() > 1
                        && pick.getItemMeta().getLore().get(1).equals("Chisel Mode (/chisel to toggle)")) {
                    lore.add("Mining Mode (/chisel to toggle)");
                    player.sendMessage("Mining mode set.");
                }
                im.setLore(lore);
                pick.setItemMeta(im);
                return true;
            } else if (cmd.getName().equalsIgnoreCase("compareprints")) {
                //hold a sheet in each hand and type command
                //you will get a "MATCH" or "NO MATCH" if a single alt group spans both hands.
                PlayerInventory pi = player.getInventory();
                if (pi.getItemInMainHand().hasItemMeta() && pi.getItemInMainHand().getItemMeta().hasLore() && pi.getItemInMainHand().getItemMeta().getLore().get(0).contains("A sheet of fingerprints")
                        && pi.getItemInOffHand().hasItemMeta() && pi.getItemInOffHand().getItemMeta().hasLore() && pi.getItemInOffHand().getItemMeta().getLore().get(0).contains("A sheet of fingerprints")) {
                    ItemStack sheet1 = pi.getItemInMainHand();
                    ItemStack sheet2 = pi.getItemInOffHand();
                    UUID uuidA = this.plugin.fingerprintMapReverse.get(sheet1.getItemMeta().getLore().get(2));
                    UUID uuidB = this.plugin.fingerprintMapReverse.get(sheet2.getItemMeta().getLore().get(2));
                    if (uuidA == null || uuidB == null) {
                        player.sendMessage(ChatColor.RED + "Internal Error: One of these fingerprint sheets has corrupted data, please see an admin.");
                        return true;
                    }
                    boolean compareResult = this.plugin.compareAlts(uuidA, uuidB);
                    if (compareResult == true) {
                        player.sendMessage(ChatColor.DARK_GREEN + "These fingerprints are a MATCH.");
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "These fingerprints are NOT a match.");
                    }
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("createprints")) {
                //need a piece of paper and ANY type of dye in your inventory, and it will use them and create a fingerprint sheet with your own prints.
                //OR hold an exilepearl and it will make one for that person instead of you.
                //The sheet mentions the name of the person and the 8 hex code for them and that it's a fingerprint sheet
                String fakeName = null;
                if (args != null && args.length > 0 && args[0] != null) {
                    fakeName = args[0];
                }

                PlayerInventory pi = player.getInventory();
                if (pi.getItemInMainHand().getType() == Material.ENDER_PEARL && pi.getItemInMainHand().hasItemMeta() && pi.getItemInMainHand().getItemMeta().hasLore() && pi.getItemInMainHand().getItemMeta().getLore().size() > 3) {
                    
                    UUID pearledPlayerUUID = null;
                    String nameLine = pi.getItemInMainHand().getItemMeta().getLore().get(1);
                    String[] parts = nameLine.split(" ");
                    String name = parts[1].substring(2,parts[1].length());
                    
                    //#######DATE CHECK
                    String dateLine = pi.getItemInMainHand().getItemMeta().getLore().get(3);
                    String[] dateParts = dateLine.split(" "); //splits the actual date from the "Exiled on:" test
                    String date = dateParts[2].substring(2,dateParts[2].length());
                    String[] dateSubParts = date.split("/"); //month day year
                    int year = Integer.parseInt(dateSubParts[2]);
                    int month = Integer.parseInt(dateSubParts[0]);
                    int day = Integer.parseInt(dateSubParts[1]);
                    if (year < 2020 || (year == 2020 && month < 3) || (year == 2020 && month == 3 && day < 9)) {
                        player.sendMessage(ChatColor.RED + "Pearls from before the fingerprint update cannot be used for fingerprints, due to retroactive privacy concerns.");
                        return true;
                    }
                    
                    player.sendMessage("prints from pearl name: " + name);
                    
                    pearledPlayerUUID = Bukkit.getOfflinePlayer(name).getUniqueId();
                    fingerPrintSheet(player, fakeName, name,pearledPlayerUUID);
                } else {
                    fingerPrintSheet(player, fakeName, player.getDisplayName(), player.getUniqueId());
                    return true;
                }
                new DelayedAsync.SaveFingerPrintDB(plugin, plugin.getConnection()).runTaskAsynchronously(plugin);
                return true;
            } else if (cmd.getName().equalsIgnoreCase("lookupprints")) {
                if (player.isOp()) {
                    if (args != null && args.length > 0 && args[0] != null) {
                        if (Bukkit.getOfflinePlayer(args[0]) == null){
                            player.sendMessage(ChatColor.RED + "Server doesn't know about this player existing (try overworld or check spelling).");
                            return true;
                        }
                        String result = this.plugin.fingerprintMap.get(Bukkit.getOfflinePlayer(args[0]).getUniqueId());
                        if (result != null) {
                            player.sendMessage(args[0] + "'s fingerprint code is: " + result);
                        } else {
                            player.sendMessage(ChatColor.RED + "Player not found.");
                        }
                        return true;
                    }
                    return false;
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                    return true;
                }
            }  else if (cmd.getName().equalsIgnoreCase("PVPsetKBV")) {
                if (player.isOp()){
                    this.plugin.knockBackEnchantMultiplierVertical = Double.parseDouble(args[0]);
                    player.sendMessage("KBV set to " + args[0]);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            }  else if (cmd.getName().equalsIgnoreCase("PVPsetKBA")) {
                if (player.isOp()){
                    this.plugin.knockBackEnchantMultiplierAway = Double.parseDouble(args[0]);
                    player.sendMessage("KBA set to " + args[0]);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            }  else if (cmd.getName().equalsIgnoreCase("PVPsetRV")) {
                if (player.isOp()){
                    this.plugin.hitVertical = Double.parseDouble(args[0]);
                    player.sendMessage("RV set to " + args[0]);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("PVPsetRA")) {
                if (player.isOp()){
                    this.plugin.awayFromYouDampener = Double.parseDouble(args[0]);
                    player.sendMessage("RA set to " + args[0]);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("PVPsetNoTicks")) {
                if (player.isOp()){
                    this.plugin.noTicks = Integer.parseInt(args[0]);
                    player.sendMessage("NoTicks set to " + args[0]);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("PVPtoggleWoodTesting")) {
                if (player.isOp()){
                    if (this.plugin.woodTestingOn){
                        this.plugin.woodTestingOn = false;
                    } else {
                        this.plugin.woodTestingOn = true;
                    }
                    player.sendMessage("Wood Testing Toggled to " + this.plugin.woodTestingOn);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("inventorydl")) { //for restoring lost inventories quickly, will be obsolete soon
                if (player.isOp()){
                    String invString = "";
                    try
                    {
                        invString = new String ( Files.readAllBytes( Paths.get(plugin.rootBoxDirectory + "/manual_inventory_dump.txt") ) );
                        PlayerInventory inv = player.getInventory();
                        SekkritClass.deserializeInventory(inv,invString); //deprecated soon, and gives away possibly sensitive formatting
                    } 
                    catch (IOException e) 
                    {
                        e.printStackTrace();
                    }
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("ispbl")) { //for blacklisting an ISP while waiting for the next restart to pull it from then on from config
                if (player.isOp()){
                    String connection = "";
                    for (int i = 0; i < args.length; i++){
                        connection = connection + args[i];
                        if (i < args.length){
                            connection = connection + " ";
                        }
                    }
                    for (String s : plugin.avgProxies.keySet()){
                        if (s != null && s.contains(connection)){
                            plugin.getLogger().info("key: " + s + " new val: 3.0, old val: " + plugin.avgProxies.get(s));
                            plugin.avgProxies.put(s,3.0);
                            player.sendMessage("Added temp blacklist 3.0 to: " + s);
                        }
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("removethorns")) {
                if (player.isOp()){
                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    if (heldItem.hasItemMeta() && heldItem.getItemMeta().hasEnchant(Enchantment.THORNS)){
                        heldItem.removeEnchantment(Enchantment.THORNS);
                        player.sendMessage("Removed thorns.");
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("vote")) {
                player.sendMessage(ChatColor.DARK_GREEN + "Allow time for your new balance to show in between votes.");
                player.sendMessage(ChatColor.DARK_GREEN + "MCSL, worth 0.75 stamina: " + ChatColor.GREEN + "https://minecraft-server-list.com/server/447449/vote/");
                player.sendMessage(ChatColor.DARK_GREEN + "PlanetMinecraft, worth 1.0 stamina: " + ChatColor.GREEN + "https://www.planetminecraft.com/server/civrealms/vote/");
                player.sendMessage(ChatColor.DARK_GREEN + "MinecraftServers.org, worth 0.75 stamina: " + ChatColor.GREEN + "https://minecraftservers.org/vote/584299");
                
                return true;
            } else if (cmd.getName().equalsIgnoreCase("main")) {
                new SetMainCharacter(plugin,player).runTaskAsynchronously(plugin);
                return true;
            } else if (cmd.getName().equalsIgnoreCase("slowloop")) {
                if (player.isOp()){
                    if (args.length == 1){
                        try{
                            plugin.slowLoopTicks = Integer.parseInt(args[0]);
                            player.sendMessage("Slowloop set to " + args[0] + " ticks (default 200).");
                        } catch (Exception e){
                            e.printStackTrace();
                            player.sendMessage(ChatColor.RED + "Must be an integer (ticks).");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Needs an integer only after the command (ticks).");
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            } else if (cmd.getName().equalsIgnoreCase("noIPGracePeriod")) {
                if (player.isOp()){
                    if (args.length == 1){
                        try{
                            plugin.noIPGracePeriod = Integer.parseInt(args[0]);
                            player.sendMessage("No IP Grace Period set to " + args[0] + " millis (default 8000).");
                        } catch (Exception e){
                            e.printStackTrace();
                            player.sendMessage(ChatColor.RED + "Must be an integer (millis).");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Needs an integer only after the command (millis).");
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            }  else if (cmd.getName().equalsIgnoreCase("dust")) {
                if (dustCooldown.containsKey(player.getUniqueId()) && System.currentTimeMillis() - dustCooldown.get(player.getUniqueId()) < 30000){
                    player.sendMessage(ChatColor.RED + "You can only dust for prints once every 30 seconds.");
                    return true;
                } else {
                    if (plugin.checkInventory(player,Material.BOOK_AND_QUILL)){
                        ItemStack ink = player.getInventory().getItem(player.getInventory().first(Material.BOOK_AND_QUILL));
                        ink.setAmount(0);
                        dustCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                        new DustForPrints(plugin,player,player.getLocation().getBlockX(),player.getLocation().getBlockY(),player.getLocation().getBlockZ()).runTaskAsynchronously(plugin);
                    } else {
                        player.sendMessage(ChatColor.RED + "Dusting for prints requires one book and quill.");
                    }
                }
                return true;
            } else if (cmd.getName().equalsIgnoreCase("ispwl")){
                if (player.isOp()){
                    if (args.length > 0){
                        String connection = "";
                        boolean first = true;
                        for (String arg : args){
                            if (!first){
                                connection = connection + " ";
                            }
                            connection = connection + arg;
                            first = false;
                        }
                        new WhitelistISP(plugin,connection).runTaskAsynchronously(plugin);
                        player.sendMessage("ISP whitelisted.");
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "Must write out an (exact) connection string to whitelist.");
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                    return true;
                }
            }
        }
        return false;
    }
    
    public void fingerPrintSheet(Player pCommand, String fakeName, String realName, UUID uid) { //realname is because for some reason offlineplayer.getname always gives null no matter what...
        PlayerInventory pi = pCommand.getInventory();
        if (plugin.checkInventory(pCommand,Material.PAPER) && plugin.checkInventory(pCommand,Material.INK_SACK)){
            ItemStack ink = pi.getItem(pi.first(Material.INK_SACK));
            ItemStack paper = pi.getItem(pi.first(Material.PAPER));
            ink.setAmount(ink.getAmount()-1);
            paper.setAmount(paper.getAmount()-1);
            ItemStack is = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("Fingerprints");
            String playerName = realName;
            if (fakeName != null){
                playerName = fakeName;
            }
            String la[] = new String[]{"A sheet of fingerprints", "for " + playerName,plugin.getFingerPrintCode(uid)};
            List<String> lorearray = Arrays.asList(la);
            meta.setLore(lorearray);
            is.setItemMeta(meta);
            pi.addItem(is);
            
            //broadcast
            Location pCommandLoc = pCommand.getLocation();
            for (Player anyPlayer : pCommand.getWorld().getPlayers()){
                if (anyPlayer.isOnline() && pCommandLoc.distance(anyPlayer.getLocation()) < 10 && !anyPlayer.equals(pCommand) && !anyPlayer.isDead()){
                    if (uid.equals(pCommand.getUniqueId())){
                        if (fakeName != null){ //it's their own prints under a fake name
                            anyPlayer.sendMessage(ChatColor.AQUA + "You witness " + pCommand.getDisplayName() + " ink their own fingerprints [code: " + plugin.getFingerPrintCode(uid) + "] under a " + ChatColor.RED + "false name" + ChatColor.AQUA + " of '" + fakeName +"'.");
                        } else { //it's their own prints under their real name
                            anyPlayer.sendMessage(ChatColor.AQUA + "You witness " + pCommand.getDisplayName() + " ink their own fingerprints [code: " + plugin.getFingerPrintCode(uid) + "] under their " + ChatColor.DARK_GREEN + "own name" + ChatColor.AQUA + ".");
                        }
                    } else {
                        if (fakeName != null){ //it's a prisoner's prints under a fake name
                            anyPlayer.sendMessage(ChatColor.AQUA + "You witness " + pCommand.getDisplayName() + " ink a prisoner's fingerprints [prisoner: " + realName
                                    + " code: " + plugin.getFingerPrintCode(uid) + "] under a " + ChatColor.RED + "false name" + ChatColor.AQUA + " of '" + fakeName +"'.");
                        } else { //it's a prisoner's prints under the prisoner's real name
                            anyPlayer.sendMessage(ChatColor.AQUA + "You witness " + pCommand.getDisplayName() + " ink a prisoner's fingerprints [prisoner: " + realName
                                    + " code: " + this.plugin.getFingerPrintCode(uid) + "] under the prisoner's " + ChatColor.DARK_GREEN + "real name" + ChatColor.AQUA + ".");
                        }
                    }
                }
            }
        } else {
            pCommand.sendMessage(ChatColor.RED+"You need to have at least one dye and one piece of paper in your inventory to do that.");
        }
    }
}
