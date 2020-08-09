package com.civrealms.crgpve;

//java
import com.civrealms.crgmain.CivRealmsGlue;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.ChatColor;

//events
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import vg.civcraft.mc.citadel.events.ReinforcementDamageEvent;

//bukkit other
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class BlockBreakListenerNonCrops implements Listener {

	Random rand = new Random();
	private CivRealmsGlue plugin;
	public static Logger LOG = Logger.getLogger("CivRealmsGlue");
    
    public BlockBreakListenerNonCrops(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}

	String NSN = "none";

	public ItemStack alum(int amt) {
		ItemStack is = new ItemStack(Material.QUARTZ, amt);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Alum");
		String la[] = new String[] { "Add to Animal Skin", "to make Leather" };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void spawn(CreatureSpawnEvent event) {
		if (NSN != "none") {
			event.getEntity().setCustomName(NSN);
			NSN = "none";
		}
	}

	private boolean checkExploitCounter(Player p) {
		boolean exploitExemption = false;
		//currently unused
		return exploitExemption;
	}
    
    public void scarab(Block b, Player p){
        Double rnd = Math.random();
        boolean multi = false;
        if (rnd < plugin.scarabChance) {
            if (b.getWorld().getName().contains("aqua")){
                NSN = "Scarab";
                b.getLocation().getWorld().spawnEntity(b.getLocation().add(0.5,0.5,0.5),
                    EntityType.ENDERMITE);
                rnd = Math.random();
                if (rnd < 0.5 && b.getLightLevel() == 0){
                    NSN = "Scarab";
                    b.getLocation().getWorld().spawnEntity(b.getLocation().add(0.5,0.5,0.5),
                        EntityType.ENDERMITE);
                    multi = true;
                }
                List<Entity> neighbors = (List<Entity>)(b.getWorld().getNearbyEntities(b.getLocation(), 50, 50, 50));
                int nonLivingCount = 0;
                for (Entity e : neighbors){
                    if (!(e instanceof LivingEntity)){
                        nonLivingCount++;
                    }
                }
                if (multi){
                    p.sendMessage(ChatColor.RED + "You have disturbed a nest of scarabs! Use light to reduce the chance of nests.");
                } else {
                    p.sendMessage(ChatColor.RED + "You have disturbed a scarab!");
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onWebReinforcementDamage(ReinforcementDamageEvent event) { 
        if (event.getBlock().getType() == Material.WEB){
            ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
            double levelEnchant = 0.0;
            if (tool != null && tool.getEnchantments() != null && tool.getEnchantments().containsKey(Enchantment.DAMAGE_ARTHROPODS)){
                levelEnchant = tool.getEnchantmentLevel(Enchantment.DAMAGE_ARTHROPODS);
            }
            if (levelEnchant > 0){
                event.getReinforcement().setDurability(event.getReinforcement().getDurability() - 10*(int)levelEnchant + 1);
            }
        }
    }
    
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockBreakNonCrops(BlockBreakEvent event) { 
        boolean isReinforced = com.civrealms.crgmain.CitadelChecks.isReinforced(event.getBlock(),event.getPlayer()); //I HAVE TRIED BEFORE TO USE EVENT PRIORITY SO THAT CITADEL HAS JUST ALREADY CANCELED THESE BEFORE THEY GET HERE, BUT SEEMS TO NOT WORK PROPERLY. TRYING THIS TOO.
        if (event.getBlock().getType() == Material.BIRCH_FENCE) {
			Block b = event.getBlock();
			int totalBlocks = 0;
			while (b.getRelative(0, 1, 0).getType() == Material.BIRCH_FENCE) {
				b.setType(Material.AIR);
				b = b.getRelative(0, 1, 0);
				totalBlocks++;
			}
			event.setDropItems(false);
			ItemStack is = new ItemStack(Material.BIRCH_FENCE, totalBlocks);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
			return;
		}
        
		if (!isReinforced && event.getBlock().getType() == Material.LEAVES || event.getBlock().getType() == Material.LEAVES_2) {
			Double rnd = Math.random();
			// chance of leaves to sticks
			if (rnd < plugin.manualBreakStickDropChance) {
				event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
						new ItemStack(Material.STICK));
			}
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.STONE || event.getBlock().getType() == Material.COBBLESTONE) { //reinforced blocks will end up just breaking one reinforce per, until the very end, instead of being a hellscape of 10xthe reinforcement with a stone pick, for example.
			boolean wasChiseled = false;
			double strength; // pick strength 1/(10-this) chance to break stone up
			if (event.getPlayer().getItemInHand().getType() == Material.LEVER) {
				wasChiseled = chisel(event.getBlock(), event);
			} else if (event.getPlayer().getItemInHand().hasItemMeta() && event.getPlayer().getItemInHand().getItemMeta().hasLore()
					&& event.getPlayer().getItemInHand().getItemMeta().getLore().size() > 1 && event.getPlayer()
							.getItemInHand().getItemMeta().getLore().get(1).equals("Chisel Mode (/chisel to toggle)")) {
				wasChiseled = chisel(event.getBlock(), event);
			}
			if (!wasChiseled) {
                if (event.getPlayer().getItemInHand().getType() == Material.DIAMOND_PICKAXE){
                    scarab(event.getBlock(), event.getPlayer());
                    return;
                } else {
                    if (event.getPlayer().getItemInHand().getType() != Material.GOLD_PICKAXE
                            && event.getPlayer().getItemInHand().getType() != Material.IRON_PICKAXE) {
                        strength = plugin.stoneStrength; // stone pick
                        if (event.getBlock().getType() == Material.STONE) {
                            if (rand.nextDouble() < (1 / (10 - strength))) {
                                if (event.getBlock().getData() == 0) {
                                    event.setCancelled(true);
                                    event.getBlock().setType(Material.COBBLESTONE);
                                }
                                // else granite, diorite, andesite, just drop themselves, weaker than stone.
                                // Still caveins
                            } else {
                                event.setCancelled(true);
                                return; // never make more cobblestone next to it no matter what when it's stone, best
                                        // you can do is turn THIS block to cobblestone
                            }
                        } else if (rand.nextDouble() < plugin.stonePickCobbleFailChance) {
                            event.setCancelled(true);
                            return; // don't make more cobblestone or anything, you just failed
                        }
                        // but 1/3 of the time if cobble, it won't return, or cancel, so it wlil break
                        // and continue on to the code below.
                    } else if (event.getPlayer().getItemInHand().getType() == Material.GOLD_PICKAXE
                            || event.getPlayer().getItemInHand().getType() == Material.IRON_PICKAXE) {
                        if (event.getBlock().getType() == Material.STONE) {
                            strength = plugin.bronzeStrength; // bronze
                            if (event.getPlayer().getItemInHand().getType() == Material.IRON_PICKAXE) {
                                strength = plugin.ironStrength; // iron
                                if (event.getPlayer().getItemInHand().getItemMeta().hasLore()
                                        && event.getPlayer().getItemInHand().getItemMeta().getLore().size() > 1
                                        && event.getPlayer().getItemInHand().getItemMeta().getLore().get(0)
                                                .equals("Made of tempered steel")) {
                                    strength = plugin.steelStrength; // steel
                                }
                            }
                            if (rand.nextDouble() < (1 / (10 - strength))) { // 1/5 iron, 1/4 bronze, 1/3 steel (in addition
                                                                                // to speed diffs), primitive is 1/10
                                if (event.getBlock().getData() == 0) {
                                    event.setCancelled(true);
                                    event.getBlock().setType(Material.COBBLESTONE);
                                }
                                // else granite, diorite, andesite, just drop themselves, weaker than stone.
                                // Still caveins
                            } else {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                    
                    //endermites
                    scarab(event.getBlock(), event.getPlayer());
                    
                    int x = event.getBlock().getX();
                    int y = event.getBlock().getY();
                    int z = event.getBlock().getZ();
                    Block block = event.getBlock();

                    if (rand.nextDouble() > (double) plugin.caveinChance) {
                        destabilizeRock(x, y, z, block, false, checkExploitCounter(event.getPlayer())); // regular
                    } else {
                        for (int x2 = -1 * (int) (plugin.caveinRadius - 0.5); x2 < (int) (plugin.caveinRadius + 0.5); x2++) {
                            for (int z2 = -1 * (int) (plugin.caveinRadius - 0.5); z2 < (int) (plugin.caveinRadius + 0.5); z2++) {
                                if (x2 * x2 + z2 * z2 < plugin.caveinRadius * plugin.caveinRadius) {
                                    for (int y2 = plugin.caveinDown; y2 < plugin.caveinUp; y2++) {
                                        if (block.getRelative(x2, y2 - 1, z2).getType().equals(Material.AIR)) {
                                            cobbleize(block.getRelative(x2, y2, z2), true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
			}
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.QUARTZ_ORE) {
			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), alum(1));
			destabilizeRock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock(),
					false, checkExploitCounter(event.getPlayer()));
			return;
        } else if (!isReinforced && event.getBlock().getType() == Material.CLAY) {
            if (!event.getBlock().getWorld().getName().contains("aqua") && !event.getBlock().getWorld().getName().contains("prison")){
                event.setDropItems(false);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.CLAY_BALL,2));
                event.getBlock().setType(Material.AIR);
            }
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.FURNACE) {
			ItemStack is = new ItemStack(Material.FURNACE, 1);
			ItemMeta meta = is.getItemMeta();
			meta.setDisplayName("Oven");
			String la[] = new String[] { "A simple oven for low temperature tasks" };
			List<String> lorearray = Arrays.asList(la);
			meta.setLore(lorearray);
			is.setItemMeta(meta);

			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.IRON_BLOCK) {
			ItemStack is = new ItemStack(Material.IRON_BLOCK, 1);
			event.setDropItems(false);
			// event.setDropItems(false);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
            event.getBlock().setType(Material.AIR);
            LOG.info("CRG, Iron Block Dropped at " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + " by player " + event.getPlayer().getDisplayName());
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.EMERALD_ORE) {
			ItemStack is = new ItemStack(Material.EMERALD_ORE, 1);
			ItemMeta meta = is.getItemMeta();
			meta.setDisplayName("Copper Ore");
			String la[] = new String[] { "Combine with tin ore to make bronze" };
			List<String> lorearray = Arrays.asList(la);
			meta.setLore(lorearray);
			is.setItemMeta(meta);
			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
			destabilizeRock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock(),
					false, checkExploitCounter(event.getPlayer()));
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.COAL_ORE) {
			ItemStack is = new ItemStack(Material.COAL_ORE, 1);
			ItemMeta meta = is.getItemMeta();
			meta.setDisplayName("Tin Ore");
			String la[] = new String[] { "Combine with copper ore to make bronze" };
			List<String> lorearray = Arrays.asList(la);
			meta.setLore(lorearray);
			is.setItemMeta(meta);
			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			// event.setDropItems(false);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
			destabilizeRock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock(),
					false, checkExploitCounter(event.getPlayer()));
			return;
		} else if (!isReinforced && (event.getBlock().getType() == Material.LAPIS_ORE
				|| event.getBlock().getType() == Material.DIAMOND_ORE
				|| event.getBlock().getType() == Material.REDSTONE_ORE
				|| event.getBlock().getType() == Material.COAL_BLOCK
				|| event.getBlock().getType() == Material.GLOWING_REDSTONE_ORE
				|| event.getBlock().getType() == Material.GOLD_ORE || event.getBlock().getType() == Material.IRON_ORE)) {

			destabilizeRock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock(),
					false, checkExploitCounter(event.getPlayer()));
            if (event.getBlock().getType() == Material.DIAMOND_ORE && !event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)){
                ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
                double levelEnchant = 0.0;
                if (tool != null && tool.getEnchantments() != null && tool.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)){
                    levelEnchant = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
                }
                int bonus = 0;
                if (Math.random()<0.25*levelEnchant){
                    bonus = 1;
                }
                ItemStack is = new ItemStack(Material.DIAMOND, 1+bonus);
                event.getBlock().setType(Material.AIR);
                event.setCancelled(true); //NO
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                return;
            }
		} else if (!isReinforced && event.getBlock().getType() == Material.ICE) {
			ItemStack is = new ItemStack(Material.ICE, 1);
			event.setDropItems(false);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
            event.getBlock().setType(Material.AIR);
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.GLOWSTONE) {
			ItemStack is = new ItemStack(Material.GLOWSTONE, 1);
			event.setDropItems(false);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
            event.getBlock().setType(Material.AIR);
            LOG.info("CRG, Glowstone Dropped at " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + " by player " + event.getPlayer().getDisplayName());
			return;
		} else if (!isReinforced && event.getBlock().getType() == Material.GOLD_BLOCK) {
			ItemStack is = new ItemStack(Material.GOLD_BLOCK, 1);
			event.setDropItems(false);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
            event.getBlock().setType(Material.AIR);
            LOG.info("CRG, Gold Block Dropped at " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + " by player " + event.getPlayer().getDisplayName());
			return;
		} 
		if (!isReinforced && event.getBlock().getType() == Material.SAND) {
			Double rnd = Math.random();
			if (rnd < plugin.scorpionChance) {
				if (event.getBlock().getBiome() == Biome.DESERT || event.getBlock().getBiome() == Biome.MESA
						|| event.getBlock().getBiome() == Biome.DESERT_HILLS
						|| event.getBlock().getBiome() == Biome.MESA_CLEAR_ROCK
						|| event.getBlock().getBiome() == Biome.SWAMPLAND
						|| event.getBlock().getBiome() == Biome.MUTATED_SWAMPLAND) {
					NSN = "Scorpion";
					event.getBlock().getLocation().getWorld().spawnEntity(event.getBlock().getLocation().add(0.5,0.5,0.5),
							EntityType.SILVERFISH);
				}
			}
		} 
	}

	private boolean chisel(Block block, BlockBreakEvent event) { // boolean is if it's a chisel relevant block
		if (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().isReinforced(event.getBlock())
                && !((vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(event.getBlock()) instanceof vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)
                    && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)(vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(event.getBlock()))).canBypass(event.getPlayer())
                    && vg.civcraft.mc.citadel.PlayerState.get(event.getPlayer()).isBypassMode())
                && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(event.getBlock())).getHealth() > 0.05) {
			return false;
		}
        if (event.getBlock().getType() == Material.COBBLESTONE) {
			event.setCancelled(true);
			event.getBlock().setType(Material.STONE);
			return true;
		} else if (event.getBlock().getType() == Material.STONE) {
			if (event.getBlock().getData() == 1) { // granite into polished granite
				event.setCancelled(true);
				event.getBlock().setData((byte) 2);
				return true;
			} else if (event.getBlock().getData() == 3) { // diorite into polished diorite
				event.setCancelled(true);
				event.getBlock().setData((byte) 4);
				return true;
			} else if (event.getBlock().getData() == 5) { // andesite into polished andesite
				event.setCancelled(true);
				event.getBlock().setData((byte) 6);
				return true;
			}
		}
		return false;
	}

	private void cobbleize(Block block, boolean cavein) {
		if (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().isReinforced(block)) {
			return;
		}
		if (block.getType().equals(Material.STONE) || block.getType().equals(Material.COBBLESTONE)) {
			if (rand.nextDouble() < plugin.cobbleContagionChance) {
				if (block.getRelative(0, -1, 0).getType().equals(Material.AIR)) {
					FallingBlock fb = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5),
							Material.COBBLESTONE, (byte) 0);
					fb.setHurtEntities(true);
					block.setType(Material.AIR);
					if (cavein) {
						if (block.getRelative(0, 1, 0).getType().equals(Material.STONE)
								|| block.getRelative(0, 1, 0).getType().equals(Material.COBBLESTONE)) {
							fb = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 1.2, 0.5),
									Material.COBBLESTONE, (byte) 0);
							fb.setHurtEntities(true);
							block.getRelative(0, 1, 0).setType(Material.AIR);
						}
					}
				} else {
					block.setType(Material.COBBLESTONE);
				}
			}
		}
		if ((block.getType() == Material.STONE && block.getData() == 0) || block.getType() == Material.COBBLESTONE) {
			if (rand.nextDouble() < plugin.cobbleContagionChance) {
				if (block.getRelative(0, -1, 0).getType().equals(Material.AIR)) {
					FallingBlock fb = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5),
							Material.COBBLESTONE, (byte) 0);
					fb.setHurtEntities(true);
					block.setType(Material.AIR);
					if (cavein) {
						if (block.getRelative(0, 1, 0).getType().equals(Material.STONE)
								|| block.getRelative(0, 1, 0).getType().equals(Material.COBBLESTONE)) {
							fb = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 1.2, 0.5),
									Material.COBBLESTONE, (byte) 0);
							fb.setHurtEntities(true);
							block.getRelative(0, 1, 0).setType(Material.AIR);
						}
					}
				} else {
					block.setType(Material.COBBLESTONE);
				}
			}
		}
	}

	private void destabilizeRock(int x, int y, int z, Block block, boolean cavein, boolean exploitExemption) {
		if (!exploitExemption) {
			Block tempBlock;
			tempBlock = block.getWorld().getBlockAt(x, y, z + 1);
			cobbleize(tempBlock, cavein);
			tempBlock = block.getWorld().getBlockAt(x, y, z - 1);
			cobbleize(tempBlock, cavein);
			tempBlock = block.getWorld().getBlockAt(x, y + 1, z);
			cobbleize(tempBlock, cavein);
			tempBlock = block.getWorld().getBlockAt(x, y - 1, z);
			cobbleize(tempBlock, cavein);
			tempBlock = block.getWorld().getBlockAt(x + 1, y, z);
			cobbleize(tempBlock, cavein);
			tempBlock = block.getWorld().getBlockAt(x - 1, y, z);
			cobbleize(tempBlock, cavein);
		}
	}

}
