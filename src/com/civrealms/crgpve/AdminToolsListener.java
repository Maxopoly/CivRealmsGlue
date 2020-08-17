package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import static com.civrealms.crgpve.PVEMiscellaneousListener.LOG;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;

/**
 * @author Crimeo
 */
public class AdminToolsListener  implements Listener {
    private CivRealmsGlue plugin;
    private ArrayList<Player> players = new ArrayList<>();
    private HashMap<UUID, Integer> sepIndex = new HashMap<>();
    
    public AdminToolsListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZeus(ProjectileHitEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (event.getEntity().getType().equals(EntityType.SPECTRAL_ARROW) && shooter != null && shooter instanceof Player && ((Player)shooter).isOp()){
            event.getEntity().getWorld().strikeLightning(event.getEntity().getLocation());
            event.getEntity().remove();
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void OnAdminClickUtilities(PlayerInteractEvent event) {
		if (event.getPlayer().isOp()) {
            if ((event.getAction().name().equals("LEFT_CLICK_AIR") || event.getAction().name().equals("LEFT_CLICK_BLOCK")) && event.getHand() == EquipmentSlot.HAND){
                if (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BOOK)){
                    //temporary debug stuff that i don't feel like making commands for
                    LOG.info("Name: " + event.getPlayer().getName());
                    LOG.info("Custom Name: " + event.getPlayer().getCustomName());
                    LOG.info("Display Name: " + event.getPlayer().getDisplayName());
                    LOG.info("Player List Name: " + event.getPlayer().getPlayerListName());
                    LOG.info("UUID: " + event.getPlayer().getUniqueId());
                    return;
                }
            }
            //quick game mode change
            if ((event.getAction().name().equals("LEFT_CLICK_AIR") || event.getAction().name().equals("LEFT_CLICK_BLOCK")) && event.getHand() == EquipmentSlot.HAND){
                if(event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)){
                    event.getPlayer().setGameMode(GameMode.CREATIVE);
                } else if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.FEATHER)){
                    event.getPlayer().setGameMode(GameMode.SURVIVAL);
                }
                return;
            }
            if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.FEATHER)) {
				if (event.getAction().name().equals("RIGHT_CLICK_BLOCK") || event.getAction().name().equals("RIGHT_CLICK_AIR") && event.getHand() == EquipmentSlot.HAND) {
					if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)){
                        event.getPlayer().setGameMode(GameMode.SPECTATOR);
                        return;
                    } else if (event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)){
                        event.getPlayer().setGameMode(GameMode.CREATIVE);
                        return;
                    }
				}
			}
            //Replicate spectator mode chest spying but while just vanished too
			if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.EYE_OF_ENDER) && event.getHand() == EquipmentSlot.HAND) {
				if (event.getAction().name().equals("RIGHT_CLICK_BLOCK")) {
					if (event.getClickedBlock().getType().equals(Material.CHEST)
							|| event.getClickedBlock().getType().equals(Material.TRAPPED_CHEST)) {
						Inventory dummyInventory = Bukkit.createInventory(null, 54, "DummyChest");
						dummyInventory
								.setContents(((Chest) event.getClickedBlock().getState()).getInventory().getContents());
						event.getPlayer().openInventory(dummyInventory);
						event.setCancelled(true);
                        return;
					}
				}
			}
            //Seed plants from a distance
			if (event.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SAPLING)) {
                Set<Material> transparents = null;
				if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NETHER_STALK)) {
					List<Block> lastTwo = event.getPlayer().getLastTwoTargetBlocks(transparents, 400);
					if (!lastTwo.get(1).getType().equals(Material.NETHER_WARTS) && lastTwo.get(1).getType().isSolid()
							&& !lastTwo.get(1).getType().equals(Material.LEAVES)
							&& !lastTwo.get(1).getType().equals(Material.LEAVES_2)) {
						lastTwo.get(1).setType(Material.SOUL_SAND);
						lastTwo.get(1).getRelative(BlockFace.UP).setType(Material.NETHER_WARTS);
					}
				} else if (event.getPlayer().getInventory().getItemInMainHand().getType()
						.equals(Material.BEETROOT_SEEDS)) {
					List<Block> lastTwo = event.getPlayer().getLastTwoTargetBlocks(transparents, 400);
					if (!lastTwo.get(1).getType().equals(Material.BEETROOT_BLOCK) && lastTwo.get(1).getType().isSolid()
							&& !lastTwo.get(1).getType().equals(Material.LEAVES)
							&& !lastTwo.get(1).getType().equals(Material.LEAVES_2)) {
						lastTwo.get(1).setType(Material.SOIL);
						lastTwo.get(1).getRelative(BlockFace.UP).setType(Material.BEETROOT_BLOCK);
					}
				} else if (event.getPlayer().getInventory().getItemInMainHand().getType()
						.equals(Material.CARROT_ITEM)) {
					List<Block> lastTwo = event.getPlayer().getLastTwoTargetBlocks(transparents, 400);
					if (!lastTwo.get(1).getType().equals(Material.CARROT) && lastTwo.get(1).getType().isSolid()
							&& !lastTwo.get(1).getType().equals(Material.LEAVES)
							&& !lastTwo.get(1).getType().equals(Material.LEAVES_2)) {
						lastTwo.get(1).setType(Material.SOIL);
						lastTwo.get(1).getRelative(BlockFace.UP).setType(Material.CARROT);
					}
				} else if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.CACTUS)) {
					List<Block> lastTwo = event.getPlayer().getLastTwoTargetBlocks(transparents, 400);
					if (!lastTwo.get(1).getType().equals(Material.CACTUS) && lastTwo.get(1).getType().isSolid()
							&& !lastTwo.get(1).getType().equals(Material.LEAVES)
							&& !lastTwo.get(1).getType().equals(Material.LEAVES_2)) {
						lastTwo.get(1).setType(Material.SAND);
						if (!lastTwo.get(0).getRelative(1, 0, 0).getType().equals(Material.CACTUS)
								&& !lastTwo.get(0).getRelative(-1, 0, 0).getType().equals(Material.CACTUS)
								&& !lastTwo.get(0).getRelative(0, 0, 1).getType().equals(Material.CACTUS)
								&& !lastTwo.get(0).getRelative(0, 0, -1).getType().equals(Material.CACTUS)) {
							lastTwo.get(1).getRelative(BlockFace.UP).setType(Material.CACTUS);
						}
					}
				}
			}
		}
	}
    
    @EventHandler(priority = EventPriority.HIGHEST)
	public void info(BlockPlaceEvent event) {
		if(event.getBlock().getType() == Material.CAKE_BLOCK && event.getPlayer().isOp()){
            LOG.info("CRG SECRET CAKE INFO LOG: " + "world name: " + event.getBlock().getWorld().getName());
            LOG.info("CRG SECRET CAKE INFO LOG: " + "off hand item: " + event.getPlayer().getInventory().getItemInOffHand().getType().name());
            LOG.info("CRG SECRET CAKE INFO LOG: " + "block below: " + event.getBlock().getRelative(0,-1,0).getType().name());  
            if (event.getPlayer().isSneaking()){
                List<LivingEntity> ents = event.getBlock().getWorld().getLivingEntities();
                for (LivingEntity ent:ents){
                    if (ent instanceof Player){
                        LOG.info("DISPLAY NAME: " + ((Player)ent).getDisplayName());
                        LOG.info("REAL NAME: " + ((Player)ent).getName());
                        LOG.info("UUID: " + ((Player)ent).getUniqueId().toString());
                    }
                    if (ent.getLocation().distanceSquared(event.getBlock().getLocation()) < 2500){
                        ent.setCollidable(false);
                    }
                }
            }
        }
	}
}
