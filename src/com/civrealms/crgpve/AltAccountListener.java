package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import static com.civrealms.crgpve.PVEMiscellaneousListener.LOG;
import com.programmerdan.minecraft.banstick.data.BSPlayer;
import isaac.bastion.BastionBlock;
import isaac.bastion.event.BastionDamageEvent;
import isaac.bastion.manager.BastionBlockManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import vg.civcraft.mc.citadel.events.ReinforcementDamageEvent;

/**
 * @author Crimeo
 */
public class AltAccountListener  implements Listener {
    
    private static HashMap<UUID,Integer> altFamilies = new HashMap<UUID,Integer>();
    private static HashMap<Integer,Set<UUID>> altFamiliesInverse = new HashMap<Integer,Set<UUID>>();
    private static HashMap<String,Long> lastBlocked = new HashMap<String,Long>();
    int nextFamilyID = 0;
    private CivRealmsGlue plugin;
    
    public AltAccountListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    public static HashMap<UUID,Integer> getAltFamilies(){
        return altFamilies;
    }
    
    public static HashMap<Integer,Set<UUID>> getAltFamiliesInverse(){
        return altFamiliesInverse;
    }
    
    @EventHandler
	private void altFamilyAssignment(PlayerJoinEvent event) {
        //this isn't designed to be perfectly secure and airtight, just to stop the vast majority of alt mass farming. if they squeeze a little bit of macroing out of the first session of a fresh alt, ill worry about that later, whatever
        Set<BSPlayer> directAssoc = BSPlayer.byUUID(event.getPlayer().getUniqueId()).getTransitiveSharedPlayers(true);
        UUID joiningPlayerUUID = event.getPlayer().getUniqueId();
        for (BSPlayer alt : directAssoc){
            if (alt.getUUID() != joiningPlayerUUID && altFamilies.containsKey(alt.getUUID())){
                int familyID = altFamilies.get(alt.getUUID());
                altFamilies.put(joiningPlayerUUID, familyID);
                Set<UUID> newFamilySet = altFamiliesInverse.get(familyID);
                newFamilySet.add(joiningPlayerUUID);
                altFamiliesInverse.put(familyID, newFamilySet);
                LOG.info(event.getPlayer().getDisplayName() + " assigned to existing alt family: " + altFamilies.get(alt.getUUID()));
                return;
            }
        }
        //no alts joined yet, make a new family
        altFamilies.put(joiningPlayerUUID, nextFamilyID);
        Set<UUID> newFamilySet = new HashSet<UUID>();
        newFamilySet.add(joiningPlayerUUID);
        altFamiliesInverse.put(nextFamilyID, newFamilySet);
        LOG.info(event.getPlayer().getDisplayName() + " assigned to new alt family: " + nextFamilyID);
        nextFamilyID++;
    }
    
    //Obsolete now with no simultaneous alt placements or breaks anyway in same shard:
    /*
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void altReinforcementDamage(ReinforcementDamageEvent event) {
        Set<UUID> altSet = altFamiliesInverse.get(altFamilies.get(event.getPlayer().getUniqueId()));
        int altCount = 0;
        for (UUID altID : altSet){
            if(!(altID.equals(event.getPlayer().getUniqueId())) && lastBlocked.containsKey(altID.toString()) && System.currentTimeMillis() - lastBlocked.get(altID.toString()) < 5000){
                altCount++;
                if (altCount == 1){
                    if (com.civrealms.crgmain.CitadelChecks.isReinforced(event.getBlock(),event.getPlayer())){
                        event.setCancelled(true);
                        LOG.info("break canceled for alt mining: " + event.getPlayer().getDisplayName());
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "Alt Throttling: " + ChatColor.RED + "You must wait 5 seconds to place or break a block due to alt activity. Alt throttling allows zero tolerance for breaking non-bypass reinforced blocks.");
                        break;
                    }
                }
            }
        }
        lastBlocked.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void altBastionDamage(BastionDamageEvent event) {
        Set<UUID> altSet = altFamiliesInverse.get(altFamilies.get(event.getPlayer().getUniqueId()));
        int altCount = 0;
        for (UUID altID : altSet){
            if(!(altID.equals(event.getPlayer().getUniqueId())) && lastBlocked.containsKey(altID.toString()) && System.currentTimeMillis() - lastBlocked.get(altID.toString()) < 5000){
                altCount++;
                if (altCount == 1){
                    //bastion check
                    BastionBlockManager bbm = isaac.bastion.Bastion.getBastionManager();
                    Set<BastionBlock> blocking = bbm.getBlockingBastions(event.getBastion().getLocation());
                    if (blocking.size() > 0){
                        for (BastionBlock bb : blocking){
                            if (!bb.canPlace(event.getPlayer())){
                                event.setCancelled(true);
                                LOG.info("Block Placement canceled for alt building in unfriendly bastion field: " + event.getPlayer().getDisplayName());
                                event.getPlayer().sendMessage(ChatColor.DARK_RED + "Alt Throttling: " + ChatColor.RED + "You must wait 5 seconds to place or break a block due to alt activity. Alt throttling allows zero tolerance for placement in unfriendly bastion fields.");
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        lastBlocked.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
    }
    */
    
    //ALL BLOCK BREAKS/PLACES/BREEDS cancel for alts within 5 seconds of earlier alt activities of those
    @EventHandler(priority = EventPriority.LOWEST)
	public void altBreak(BlockBreakEvent event) {
        Set<UUID> altSet = altFamiliesInverse.get(altFamilies.get(event.getPlayer().getUniqueId()));
        int altCount = 0;
        for (UUID altID : altSet){
            if(!(altID.equals(event.getPlayer().getUniqueId())) && lastBlocked.containsKey(altID.toString()) && System.currentTimeMillis() - lastBlocked.get(altID.toString()) < 5000){
                altCount++;
                if (altCount > 0){
                    event.setCancelled(true);
                    LOG.info("break canceled for alt mining: " + event.getPlayer().getDisplayName());
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "Alt Throttling: " + ChatColor.RED + "You must wait 5 seconds to place or break a block due to alt activity.");
                    break;
                }
            }
        }
        lastBlocked.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
	}
    
    //ALL BLOCK BREAKS/PLACES/BREEDS cancel for alts within 5 seconds of earlier alt activities of those
    @EventHandler(priority = EventPriority.LOWEST)
	public void altPlace(BlockPlaceEvent event) {
        Set<UUID> altSet = altFamiliesInverse.get(altFamilies.get(event.getPlayer().getUniqueId()));
        int altCount = 0;
        for (UUID altID : altSet){
            if(!(altID.equals(event.getPlayer().getUniqueId())) && lastBlocked.containsKey(altID.toString()) && System.currentTimeMillis() - lastBlocked.get(altID.toString()) < 5000){
                altCount++;
                if (altCount > 0){
                    event.setCancelled(true);
                    LOG.info("placement canceled for alt building: " + event.getPlayer().getDisplayName());
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "Alt Throttling: " + ChatColor.RED + "You must wait 5 seconds to place or break a block due to alt activity.");
                    break;
                }
            }
        }
        lastBlocked.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
	}
    
}
