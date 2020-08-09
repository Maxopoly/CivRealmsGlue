package com.civrealms.crgpve;

//java
import com.civrealms.crgmain.CivRealmsGlue;
import com.civrealms.delayedtasks.DelayedAsync;
import com.civrealms.delayedtasks.DelayedAsync.ProcessVote;
import com.vexsoftware.votifier.model.Vote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//bukkit event management
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

//bastion
import isaac.bastion.BastionBlock;
import java.util.concurrent.CopyOnWriteArraySet;

//bukkit events
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import vg.civcraft.mc.citadel.events.ReinforcementCreationEvent;
import vg.civcraft.mc.citadel.events.ReinforcementChangeTypeEvent;

//bukkit other classes
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.Biome;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Leaves;
import org.bukkit.scheduler.BukkitRunnable;

//3rd party
//import com.untamedears.JukeAlert.events.PlayerHitSnitchEvent;
import vg.civcraft.mc.citadel.ReinforcementMode;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitTask;
 
public class PVEMiscellaneousListener implements Listener {

	Random rand = new Random();
	private CivRealmsGlue plugin;
	public static Logger LOG = Logger.getLogger("CivRealmsGlue");
    private HashMap<String, Boolean> bedMode = new HashMap<String, Boolean>(); // UUID and mode
    private HashMap<UUID,Long> postJoinTellCooldown = new HashMap<UUID,Long>();
    Block mostRecentCancel;
    Material mostRecentCancelType;
    long timeSinceLastCancel;

    private HashMap<String, Long> leafSnipeCooldown = new HashMap<String, Long>();
    private HashMap<String, Long> sheepSheared = new HashMap<String, Long>();
    private HashMap<String, Long> bastionAttempts = new HashMap<String, Long>();
    private HashMap<String, String> lastDamageType = new HashMap<String, String>(); //uuid, enum of last damage type taken
    private HashMap<String, Double> lastDamageAmount = new HashMap<String, Double>(); //uuid, damage taken
    private HashMap<String, Integer> bubbleMemory = new HashMap<String, Integer>(); //uuid, bubble meter
    private HashMap<UUID, Long> lastRanBSL = new HashMap<UUID, Long>();
    private HashMap<UUID, Long> enderFrameCooldown = new HashMap<UUID, Long>();
    private HashMap<UUID, Long> nonEnderFrameCooldown = new HashMap<UUID, Long>();
    HashMap<Material, Integer> reinforcementHierarchy = new HashMap<Material, Integer>();

    public PVEMiscellaneousListener(CivRealmsGlue plugin) {
        this.plugin = plugin;
        reinforcementHierarchy.put(Material.STONE, 1);
        reinforcementHierarchy.put(Material.IRON_INGOT, 2);
        reinforcementHierarchy.put(Material.DIAMOND, 3);
        reinforcementHierarchy.put(Material.BEDROCK, 4);
    }
    
    @EventHandler
    public void death(PlayerDeathEvent e) {
        BukkitRunnable r = new BukkitRunnable() { 
            public void run() {
                ((Player)e.getEntity()).spigot().respawn();
            }
        };
        r.runTaskLater(plugin,20);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVotifierEvent(VotifierEvent event) {
        this.plugin.getLogger().info("[CRG] Vote Received via listener: " + event.getVote());
        Vote v = event.getVote();
        String website = v.getServiceName();
        long time = v.getLocalTimestamp();
        String playerName = v.getUsername();
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player != null) {
            this.plugin.getLogger().info("[CRG]: Attempting vote, offline player is not null");
            new ProcessVote(plugin, player, time, website).runTaskAsynchronously(plugin);
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            String command = "say " + ChatColor.DARK_GREEN + "Thanks " + ChatColor.DARK_GREEN + "for " + ChatColor.DARK_GREEN 
                    + "voting, " + ChatColor.RED + playerName;
            Bukkit.dispatchCommand(console, command);
        }
    }

	private void giveKit(Player p, Biome biome) {
		if (System.currentTimeMillis() - p.getFirstPlayed() < 30000) {
			p.getInventory().addItem(new ItemStack(Material.BED, 1, (short) 12));
            
            ItemStack oldBronzePick = new ItemStack(Material.GOLD_PICKAXE, 1);
            ItemMeta pickmeta = oldBronzePick.getItemMeta();
            pickmeta.setDisplayName("Bronze Pickaxe");
            String la[] = new String[] { "Made of cast and hammered bronze","Mining Mode (/chisel to toggle)" };
            List<String> lorearray = Arrays.asList(la);
            pickmeta.setLore(lorearray);
            oldBronzePick.setItemMeta(pickmeta);
            p.getInventory().addItem(oldBronzePick);
            
            if (!p.getWorld().getName().equals("prison_the_end")) { //####remove this after bastions work there
                ItemStack claimBastion = new ItemStack(Material.ENDER_CHEST, 1);
                ItemMeta bastionmeta = claimBastion.getItemMeta();
                bastionmeta.setDisplayName("Claim Bastion");
                la = new String[] { "Claim Bastions prevent unfriendly block placements and allow easier building. 21x21 area. Place at bedrock. Reinforce (/ctf) to activate." };
                lorearray = Arrays.asList(la);
                bastionmeta.setLore(lorearray);
                claimBastion.setItemMeta(bastionmeta);
                p.getInventory().addItem(claimBastion);
            }
		}
		if (System.currentTimeMillis() - p.getFirstPlayed() < 86400000) {
			p.getInventory().addItem(new ItemStack(Material.STICK, 16));
			p.getInventory().addItem(new ItemStack(Material.FLINT, 18));
			p.getInventory().addItem(new ItemStack(Material.FISHING_ROD, 1));
			if (p.getWorld().getBlockAt(-3288, 1, -4368).getType().equals(Material.CHEST)) {
				p.getInventory().addItem(
						((Chest) (p.getWorld().getBlockAt(-3288, 1, -4368).getState())).getBlockInventory().getItem(0));
			}
			if (biome.equals(Biome.SAVANNA) || biome.equals(Biome.SAVANNA_ROCK) || biome.equals(Biome.MESA)
					|| biome.equals(Biome.MESA_CLEAR_ROCK) || biome.equals(Biome.MUTATED_SWAMPLAND)
					|| biome.equals(Biome.SWAMPLAND) || biome.equals(Biome.DESERT) || biome.equals(Biome.DESERT_HILLS)
					|| biome.equals(Biome.MUTATED_DESERT) || biome.equals(Biome.MUSHROOM_ISLAND)) {
				// no flint
				p.getInventory().addItem(new ItemStack(Material.FLINT, 18));
			}
			if (biome.equals(Biome.REDWOOD_TAIGA) || biome.equals(Biome.TAIGA_COLD) || biome.equals(Biome.ICE_MOUNTAINS)
					|| biome.equals(Biome.ICE_FLATS) || biome.equals(Biome.SWAMPLAND) || biome.equals(Biome.DESERT)
					|| biome.equals(Biome.DESERT_HILLS) || biome.equals(Biome.MUTATED_DESERT)
					|| biome.equals(Biome.MUSHROOM_ISLAND) || biome.equals(Biome.MESA)
					|| biome.equals(Biome.MESA_CLEAR_ROCK) || biome.equals(Biome.MUTATED_SWAMPLAND)) {
				// very little food
				p.getInventory().addItem(new ItemStack(Material.BREAD, 16));
			}
		} else {
			p.getInventory().addItem(new ItemStack(Material.COOKIE, 8));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void boom(BlockIgniteEvent event) {
		if (event.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL) {
			event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 3, false);
		}
	}
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    public void highVersionSwim(PlayerMoveEvent event){
        Material surroundedBy = event.getPlayer().getLocation().getBlock().getType();
        if (surroundedBy == Material.WATER || surroundedBy == Material.STATIONARY_WATER){
            if (event.getPlayer().isSneaking() && event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)){
                if (Math.abs(event.getTo().getY() - event.getFrom().getY()) > 0.15){
                    //is fast diving
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "No crouch diving.");
                    LOG.info("[CRG] Crouch diving: " + event.getPlayer().getDisplayName());
                }
            }
            double horizVelSquared = Math.pow(event.getTo().getZ() - event.getFrom().getZ(),2) + Math.pow(event.getTo().getX() - event.getFrom().getX(),2);
            if (event.getPlayer().isSprinting() && event.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && horizVelSquared < 0.025 && horizVelSquared > 0.02){
                ItemStack boots = event.getPlayer().getInventory().getBoots();
                if (boots != null && boots.getEnchantments() != null && boots.getEnchantments().containsKey(Enchantment.DEPTH_STRIDER)){
                    return;
                }
                //event.setCancelled(true);
                //event.getPlayer().sendMessage(ChatColor.RED + "No sprint swimming.");
                event.getPlayer().setSprinting(false);
                LOG.info("[CRG] Sprint Swimming: " + event.getPlayer().getDisplayName());
            }
            double dist = event.getTo().distance(event.getFrom());
        
        }
    }
    
    @EventHandler(ignoreCancelled = true)
	public void playerdeath(PlayerDeathEvent event) {
        if (event.getDeathMessage() != null){
            event.setDeathMessage("");
        }
	}
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void preProcessCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if(event.getMessage().toLowerCase().startsWith("/bsl")){
            UUID uid = event.getPlayer().getUniqueId();
            if(lastRanBSL.containsKey(uid) && System.currentTimeMillis() - lastRanBSL.get(uid) < 1800000){
                event.getPlayer().sendMessage(ChatColor.RED + "You can only run the command /bsl once per 30 minutes. We consider this to be an emergency reference source and encourage you to write down your bastion locations elsewhere and use that for more common usage.");
                //lastRanBSL.put(uid, System.currentTimeMillis());
                event.setCancelled(true);
            } else {
                lastRanBSL.put(uid, System.currentTimeMillis());
            }
        } else if (event.getPlayer().getWorld().getName().equals("world")
                    && message.toLowerCase().startsWith("/reply") || message.toLowerCase().equalsIgnoreCase("/r")
                    || message.toLowerCase().startsWith("/r ") || message.toLowerCase().startsWith("/w ")
                    || message.toLowerCase().startsWith("/msg") || message.toLowerCase().startsWith("/t ")
                    || message.toLowerCase().startsWith("/tell") || message.toLowerCase().startsWith("/pm")
                    || message.toLowerCase().equalsIgnoreCase("/w") || message.toLowerCase().startsWith("/whisper")
                    || message.toLowerCase().startsWith("/message")){
            if (!postJoinTellCooldown.containsKey(event.getPlayer().getUniqueId())){
                event.getPlayer().sendMessage(ChatColor.RED + "You must wait 15s after login to send private messages");
                postJoinTellCooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                event.setCancelled(true);
                return;
            } else if (System.currentTimeMillis() - postJoinTellCooldown.get(event.getPlayer().getUniqueId()) < 15000) {
                event.getPlayer().sendMessage(ChatColor.RED + "You must wait 15s after login to send private messages");
                event.setCancelled(true);
                return;
            } else {
                return;
            }
        }
        if(event.getMessage().toLowerCase().contains("/me ") || event.getMessage().toLowerCase().contains("/minecraft:me ") || event.getMessage().toLowerCase().contains("was slain by")){
            event.setCancelled(true);
        }
    }

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSwim(PlayerMoveEvent event) {
        Location loc = event.getTo();
        if (loc.getY() > event.getFrom().getY()) { //attempting to swim up
            if (!event.getPlayer().isInsideVehicle()) {
                if (loc.getBlock().isLiquid()
                        && !loc.getBlock().getRelative(0, -2, 0).getType().isSolid()
                        && !loc.getBlock().getRelative(0, -1, 0).getType().isSolid()
                        && event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                    String worldName = loc.getWorld().getName();
                    if (worldName.equalsIgnoreCase("ocean")){
                        return;
                    } else if (worldName.equalsIgnoreCase("realm")
                            && (Math.pow(loc.getX()-3200,2) + Math.pow(loc.getZ()+6200,2)) > 75603025){
                        return;
                    } else if (worldName.equalsIgnoreCase("Civrealms_taiga_island")
                            && (Math.pow(loc.getX()+14000,2) + Math.pow(loc.getZ()+14000,2)) > 4840000){
                        return;
                    } else if (worldName.equalsIgnoreCase("new_world")
                            && (Math.pow(loc.getX()+25646,2) + Math.pow(loc.getZ()-4128,2)) > 11902500){
                        return;
                    }
                    // check for weight:
                    ItemStack[] armor = event.getPlayer().getInventory().getArmorContents();
                    ItemStack[] inv = event.getPlayer().getInventory().getContents();
                    double weight = 0;
                    for (int i = 0; i <= 35; i++) {
                        ItemStack is = inv[i];
                        if (is != null) {
                            weight = weight + ((double) is.getAmount() / (double) is.getMaxStackSize());
                        }
                    }
                    for (int i = 0; i <= 3; i++) {
                        ItemStack is = armor[i];
                        if (is != null) {
                            weight = weight + ((double) is.getAmount() / (double) is.getMaxStackSize());
                        }
                    }
                    if (weight > plugin.swimLimit) {
                        event.getPlayer().sendMessage(
                                Double.toString(weight) + " full itemstacks is too much weight to swim up.");
                        //LOG.info("currY velocity: " + event.getPlayer().getVelocity().getY());
                        event.getPlayer().setVelocity(new Vector(0, -0.2, 0)); //was -0.3, larger numbers throw you around but aren't more effective, as they trigger more slowly too
                        event.setCancelled(true);
                    }
                }
            }
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void chorusTP(PlayerTeleportEvent event) {
		if (event.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
			event.setCancelled(true);
		}
	}
    
    @EventHandler(priority = EventPriority.NORMAL)
	public void onEatRealGapple(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() == Material.GOLDEN_APPLE && event.getItem().getDurability() == 1 && !event.getItem().hasItemMeta()){
            LOG.info("CRG: " + event.getPlayer().getDisplayName() + " attempted to eat a true god apple.");
            event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void itemSpawn(ItemSpawnEvent event) {
        if (event.getEntityType() == EntityType.DROPPED_ITEM || event.getEntity().hasMetadata("playerDrop")){
            if (event.getEntity().getName().contentEquals("item.item.egg")) {
                List<Entity> nearby = event.getEntity().getNearbyEntities(1, 1, 1);
                for (Entity e:nearby){
                    if (e instanceof Chicken){
                        if (plugin.chickenFeatherReplace < Math.random()) {
                            event.setCancelled(true);
                            event.getLocation().getWorld().dropItemNaturally(event.getLocation(), new ItemStack(Material.FEATHER));
                        } 
                        return;
                    }
                }
            }
            return;
        }
		if (!event.getEntity().getItemStack().equals(null)) {
			if (event.getEntity().getItemStack().getType().equals(Material.NETHER_STALK)
					|| event.getEntity().getItemStack().getType().equals(Material.POTATO_ITEM)
					|| event.getEntity().getItemStack().getType().equals(Material.CARROT_ITEM)
					|| (event.getEntity().getItemStack().getType().equals(Material.INK_SACK)
							&& event.getEntity().getItemStack().getData().getData() == (byte) 3)
					|| event.getEntity().getItemStack().getType().equals(Material.WHEAT)
					|| event.getEntity().getItemStack().getType().equals(Material.BEETROOT)) {

				if (!event.getEntity().getItemStack().hasItemMeta()) {
					event.setCancelled(true);
				} else {
					if (event.getEntity().getItemStack().getItemMeta().getLore().contains("Unripe")) {
						ItemMeta meta = event.getEntity().getItemStack().getItemMeta();
						ArrayList<String> lore = new ArrayList<String>(); // set lore to "Hand-Picked" only for fruits/vegetables themsleves, not dedicated seeds.
						meta.setLore(lore);
						event.getEntity().getItemStack().setItemMeta(meta);
					}
				}
			}
		}
	}

	@EventHandler
	public void scorpion(EntityDamageByEntityEvent event) {
		if (event.getDamager().getType() == EntityType.SILVERFISH) {
			((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
		}
        else if (event.getDamager().getType() == EntityType.FALLING_BLOCK && event.getEntity().getType() == EntityType.DROPPED_ITEM){
            event.setCancelled(true);
        }
	}

	@EventHandler
	public void onSnowfall(BlockFormEvent event) {
		if (event.getNewState().getType().equals(Material.SNOW)){ // && !event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 2, 2, 2).contains([snowman])) {
			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void lavaCool(BlockFormEvent event) {
		if (event.getNewState().getType().equals(Material.STONE)) {
			event.setCancelled(true);
		} else if (event.getNewState().getType().equals(Material.COBBLESTONE)) {
			event.getBlock().setType(Material.STONE);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onLeafDecay(LeavesDecayEvent event) {
        TreeSpecies ts = ((Leaves)event.getBlock().getState().getData()).getSpecies();
        Double rnd = Math.random();
        if (rnd < plugin.saplingDropChance){
            if (ts == TreeSpecies.REDWOOD){
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 1));
            } else if (ts == TreeSpecies.GENERIC){
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 0));
            } else if (ts == TreeSpecies.DARK_OAK){
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 5));
            } else if (ts == TreeSpecies.BIRCH){
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 2));
            } else if (ts == TreeSpecies.JUNGLE){
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 3));
            } else {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.SAPLING,1, (byte) 4));
            }
        }
		rnd = Math.random();
		if (rnd < plugin.stickDropChance) {
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.STICK));
		}
        if (rnd > (1-plugin.appleDropChance) && (((Leaves)event.getBlock().getState().getData()).getSpecies()==TreeSpecies.GENERIC || ((Leaves)event.getBlock().getState().getData()).getSpecies()==TreeSpecies.DARK_OAK)) {
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
				new ItemStack(Material.APPLE));
		}
        event.getBlock().setType(Material.AIR);
        event.setCancelled(true);
	}
    
    @EventHandler
	public void prepareCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getType() == InventoryType.CRAFTING
				|| event.getInventory().getType() == InventoryType.WORKBENCH) {
            //1,2,3 are across the top. 4 is the second row slot for an axe. Slot 5 is the center for swords
            Inventory inv = event.getInventory();
            if (inv.getItem(5) != null && inv.getItem(5).getType().equals(Material.POTION) && inv.getItem(5).getAmount() == 1 && inv.getItem(5).hasItemMeta() && inv.getItem(5).getItemMeta().hasDisplayName() && inv.getItem(5).getItemMeta().getDisplayName().contains("§bSmooth Vodka")){
                ItemStack is = new ItemStack(Material.LINGERING_POTION, 1);
                PotionMeta meta = (PotionMeta)is.getItemMeta();
                meta.setDisplayName("Neutral Spirits");
                PotionData pd = new PotionData(PotionType.THICK,false,false);
                meta.setBasePotionData(pd);
                is.setItemMeta(meta);
                event.getInventory().setResult(is);
                return;
            }
        }
    }

	@EventHandler(priority = EventPriority.HIGHEST)
	public void cancelRepair(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.CRAFTING
				|| event.getInventory().getType() == InventoryType.WORKBENCH) {
			if (event.getSlotType() == InventoryType.SlotType.RESULT) {
				for (int slot = 1; slot < (event.getInventory().getSize()); slot++) {
					if (event.getInventory().getItem(slot) != null) {
						if (event.getInventory().getItem(slot).hasItemMeta() && event.getInventory().getItem(slot).getItemMeta().hasLore() && !event.getInventory().getItem(slot).getItemMeta().getLore().contains("Unripe") ) {
							// exceptions for things with lore:
                            Material clickedType = event.getInventory().getItem(slot).getType();
                            if (clickedType.equals(Material.PRISMARINE_SHARD)){
                                continue; //not a dealbreaker, but since unlored prismarine IS a dealbreaker, keep checking all other slots.
                            }
							if (clickedType.equals(Material.WHEAT)
									|| clickedType.equals(Material.NETHER_STALK)
									|| clickedType.equals(Material.CARROT_ITEM)
									|| clickedType.equals(Material.QUARTZ)
									|| clickedType.equals(Material.ROTTEN_FLESH)
									|| clickedType.equals(Material.BANNER)
									|| clickedType.equals(Material.WRITTEN_BOOK)
									|| clickedType.equals(Material.MAP)
									|| clickedType.equals(Material.FIREWORK_CHARGE)
									|| clickedType.equals(Material.GLOWSTONE_DUST)
									|| clickedType.equals(Material.LEATHER_CHESTPLATE)
									|| clickedType.equals(Material.LEATHER_BOOTS)
									|| clickedType.equals(Material.LEATHER_HELMET)
									|| clickedType.equals(Material.LEATHER_LEGGINGS)
                                    || clickedType.equals(Material.POTION)
                                    || clickedType.equals(Material.GOLD_INGOT)
                                    || clickedType.equals(Material.IRON_INGOT)
                                    || clickedType.equals(Material.CHORUS_FRUIT_POPPED)
                                    || clickedType.equals(Material.BEETROOT)
                                    || clickedType.equals(Material.INK_SACK)
                                    || clickedType.equals(Material.STONE_BUTTON)
                                    || clickedType.equals(Material.NETHER_STAR)) {
								// THESE ONES ARE ALLOWED TO CRAFT IF THEY HAVE LORE (except "unripe") ^
								continue;
							} else {
								event.setCancelled(true);
								this.plugin.getLogger().info("cancelled craft: " + event.getCurrentItem().getType());
								return;
							}
						} else {
                            Material clickedType = event.getInventory().getItem(slot).getType();
							// no item meta. things to block if they DON'T have lore:
							if (clickedType.equals(Material.WHEAT)
                                    || (clickedType.equals(Material.PRISMARINE_SHARD))
									|| clickedType.equals(Material.NETHER_STALK)
									|| clickedType.equals(Material.CARROT_ITEM)){
                                    //|| clickedType.equals(Material.POTION)) {
								event.setCancelled(true); // do not allow threshing or making haybales out of unripe
															// wheat, etc. although unripe wheat should be unavailable,
															// redundant check.
								this.plugin.getLogger().info("cancelled craft: " + event.getCurrentItem().getType());
								return;
							}
						}
					}
				}
			}
		}
		if (event.getInventory().getType() == InventoryType.ANVIL) {
			if (event.getSlotType() == InventoryType.SlotType.RESULT) { //you're clicking on something coming out of the anvil
				if (event.getInventory().getItem(0).hasItemMeta()) {
					if (event.getInventory().getItem(0).getItemMeta().hasLore()) { //the original to-be-repaired item has lore
						if (event.getInventory().getItem(1) != null) { //there is something in the repair slot, not just a rename
                            //check if it is iron, and if the to-be-repaired is steel, iron ingot must be steel lored
                            //cancel anything used to repair other than leather, iron, diamond, bronze
                            Material repairMaterial = event.getInventory().getItem(1).getType();
                            boolean hasmetaone = event.getInventory().getItem(1).hasItemMeta();
                            if (repairMaterial == Material.IRON_INGOT){
                                return;
                            } else if (repairMaterial == Material.LEATHER || repairMaterial == Material.GOLD_INGOT || repairMaterial == Material.DIAMOND){
                                return;
                            } else if (repairMaterial.equals(event.getInventory().getItem(0).getType()) &&
                                    hasmetaone && event.getInventory().getItem(1).getItemMeta().hasLore()
                                    && event.getInventory().getItem(0).getItemMeta().getLore().equals(event.getInventory().getItem(1).getItemMeta().getLore())){
                                return; //exactly the same item and lore, just a blending anvil action, is fine, should take into account steel and everything.
                            }
                            if (!hasmetaone){
                                LOG.info("CRG: cancelled repair, second item has no meta");
                            } else if (!event.getInventory().getItem(1).getItemMeta().hasLore()){
                                LOG.info("CRG: cancelled repair, second item has no lore");
                            } else if (!event.getInventory().getItem(0).getItemMeta().getLore().equals(event.getInventory().getItem(1).getItemMeta().getLore())){
                                LOG.info("CRG: cancelled repair, lores don't match, lore 0: " + event.getInventory().getItem(0).getItemMeta().getLore() + " lore 1: " + event.getInventory().getItem(1).getItemMeta().getLore());
                            } else {
                                LOG.info("CRG: cancelled repair... some other reason");
                            }
                            event.setCancelled(true);
						}
					}
				}
			}
		}
	}
    
    @EventHandler
	public void xpTwentyBottlesChange(PrepareAnvilEvent event) {
        if (event.getResult().hasItemMeta() && event.getResult().getItemMeta().hasDisplayName() && SekkritClass.possiblyMaliciousAnvilRename(event.getResult().getItemMeta().getDisplayName())){
            event.getInventory().setRepairCost(100); //avoid SQL injection, foreign languages, etc.
            return;
        }
        ItemStack isZero = event.getInventory().getItem(0);
        ItemStack isOne = event.getInventory().getItem(1);
        if (isZero == null || isOne == null){
            return;
        }
        Map<Enchantment,Integer> enchantMap;
        double totalLevelsZero = 0;
        double totalLevelsOne = 0;
        if (isZero != null && isZero.hasItemMeta() && isZero.getItemMeta().hasEnchants()){
            enchantMap = isZero.getItemMeta().getEnchants();
            for (Enchantment e : enchantMap.keySet()){
                if(e.getName() == "LOOT_BONUS_BLOCKS" || e.getName() == "LOOT_BONUS_MOBS" || e.getName() == "DAMAGE_UNDEAD" || e.getName() == "DAMAGE_ARTHROPODS"){
                    totalLevelsZero += enchantMap.get(e)*0.4;
                } else if(e.getName() == "SILK_TOUCH"){
                    totalLevelsZero += enchantMap.get(e)*2;
                } else{
                    totalLevelsZero += enchantMap.get(e);
                }
            }
        }
        if (isOne != null && isOne.hasItemMeta() && isOne.getItemMeta().hasEnchants()){
            enchantMap = isOne.getItemMeta().getEnchants();
            for (Enchantment e : enchantMap.keySet()){
                if(e.getName() == "LOOT_BONUS_BLOCKS" || e.getName() == "LOOT_BONUS_MOBS" || e.getName() == "DAMAGE_UNDEAD" || e.getName() == "DAMAGE_ARTHROPODS"){
                    totalLevelsOne += enchantMap.get(e)*0.4;
                } else if(e.getName() == "SILK_TOUCH"){
                    totalLevelsOne += enchantMap.get(e)*2;
                } else if(e.getName() == "SWEEPING_EDGE"){
                    // nothing, free
                } else{
                    totalLevelsOne += enchantMap.get(e);
                }
            }
        }
        int trueMinimum = 1;
        if (totalLevelsZero > 0 && totalLevelsOne > 0){
            trueMinimum = 10;
        }
        LOG.info("totalLevelsZero " + totalLevelsZero + " totalLevelsOne " + totalLevelsOne);
        if (totalLevelsZero + totalLevelsOne > 0){
            event.getInventory().setRepairCost(Math.max((int)(2.5*Math.min(10,Math.max(totalLevelsZero,totalLevelsOne))),trueMinimum));
        }      
    }

	@EventHandler
	public void waterEmpty(PlayerBucketEmptyEvent event) {
		if (event.getBlockClicked().getType().equals(Material.CROPS)
				|| event.getBlockClicked().getType().equals(Material.SOIL)
				|| event.getBlockClicked().getType().equals(Material.BEETROOT_BLOCK)
				|| event.getBlockClicked().getType().equals(Material.CARROT)
				|| event.getBlockClicked().getType().equals(Material.POTATO)
				|| event.getBlockClicked().getType().equals(Material.CACTUS)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWaterOnCrops(BlockFromToEvent event) {
		if (!event.getToBlock().getType().equals(Material.AIR)) {
			if (event.getToBlock().getRelative(0, -1, 0).getType().equals(Material.SOIL)
					|| event.getToBlock().getRelative(0, -1, 0).getType().equals(Material.SOUL_SAND)
					|| event.getToBlock().getType().equals(Material.CACTUS)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void miningCeil(BlockPlaceEvent event) {
		if ((event.getBlock().getLocation().getBlockY() > plugin.miningCeilLevel)
				&& (plugin.getConfig().getString("worldName").contentEquals("aqua"))) {
			if (!event.getPlayer().isOp()) {
				event.setCancelled(true);
                mostRecentCancel = event.getBlock();
                timeSinceLastCancel = System.currentTimeMillis();
			}
		}
	}
    
    @EventHandler(ignoreCancelled = true,  priority = EventPriority.LOWEST)
	public void bastionOverlap(BlockPlaceEvent event) {
        if (event.getBlock().getType().equals(Material.ENDER_CHEST)){
            if (bastionAttempts.containsKey(event.getPlayer().getUniqueId().toString()) && System.currentTimeMillis() - bastionAttempts.get(event.getPlayer().getUniqueId().toString()) < 5000){
                event.getPlayer().sendMessage(ChatColor.RED + "There is a 5 second cooldown between bastion placements.");
                event.setCancelled(true);
                return;
            } else {
                bastionAttempts.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
            }
            if (plugin.getConfig().getString("worldName").contains("new_world")){
                if (event.getBlock().getBiome() == Biome.DEEP_OCEAN){
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot place bastions too far out into the ocean in this island zone.");
                    return;
                }
            } else if (plugin.getConfig().getString("worldName").equals("CivRealms_taiga_island") && event.getBlock().getBiome() == Biome.OCEAN){
                Block center = event.getBlock();
                if (center.getRelative(50, 1, 50).getBiome() == Biome.OCEAN
                        && center.getRelative(50, 1, 0).getBiome() == Biome.OCEAN
                        && center.getRelative(50, 1, -50).getBiome() == Biome.OCEAN
                        && center.getRelative(0, 1, 50).getBiome() == Biome.OCEAN
                        && center.getRelative(0, 1, -50).getBiome() == Biome.OCEAN
                        && center.getRelative(-50, 1, 50).getBiome() == Biome.OCEAN
                        && center.getRelative(-50, 1, 0).getBiome() == Biome.OCEAN
                        && center.getRelative(-50, 1, -50).getBiome() == Biome.OCEAN){
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot place bastions too far out into the ocean in this island zone.");
                    return;
                }
            }
            //bastion overlap check
            String shortName;
            if (event.getItemInHand().getItemMeta().getLore().get(0).contains("Claim")){
                shortName = "Claim";
            } else {
                shortName = "City";
            }
            boolean cancelEvent = false;
            Set<BastionBlock> blocking1 = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock().getRelative(-10,255-event.getBlock().getY(),-10));
            
            for (BastionBlock bb : blocking1){
                if (bb != null && bb.getType().getShortName().equals(shortName)){cancelEvent = true;  break; }
            }
            Set<BastionBlock> blocking2 = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock().getRelative(-10,255-event.getBlock().getY(),10));
            for (BastionBlock bb : blocking2){
                if (bb != null && bb.getType().getShortName().equals(shortName)){cancelEvent = true;  break; }
            }
            Set<BastionBlock> blocking3 = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock().getRelative(10,255-event.getBlock().getY(),-10));
            for (BastionBlock bb : blocking3){
                if (bb != null && bb.getType().getShortName().equals(shortName)){cancelEvent = true;  break; }
            }
            Set<BastionBlock> blocking4 = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock().getRelative(10,255-event.getBlock().getY(),10));
            for (BastionBlock bb : blocking4){
                if (bb != null && bb.getType().getShortName().equals(shortName)){cancelEvent = true;  break; }
            }
            
            if (cancelEvent){
                //see if you can get
                event.setCancelled(true);
                mostRecentCancel = event.getBlock();
                mostRecentCancelType = event.getBlock().getType();
                timeSinceLastCancel = System.currentTimeMillis();
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot place a bastion within less than 10 blocks from the edge of another similar type bastion field in the X or Z dimensions.");
            }
        }
    }

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void antiPillar(BlockPlaceEvent event) {
        boolean moreInfo;
            moreInfo = false;
            Set<BastionBlock> blocking = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock());
            
            if (blocking.size() != 0){// && bgm.canPlaceBlock(event.getPlayer(), blocking)){
                boolean isBastionMature = false;
                for (BastionBlock bb : blocking){
                    Set<BastionBlock> bbSet = new CopyOnWriteArraySet<BastionBlock>();
                    bbSet.add(bb);
                    if (bb.isMature()){
                        isBastionMature = true;
                    }
                }
                boolean isTagged = false; //CombatTag
                if (this.plugin.getCPT().getTagManager().isTagged(event.getPlayer().getUniqueId())){
                    isTagged = true;
                }

                boolean isWaterBiome = false;
                Biome biome = event.getBlock().getBiome();
                if (biome.equals(Biome.OCEAN) || biome.equals(Biome.FROZEN_OCEAN) || biome.equals(Biome.DEEP_OCEAN) || biome.equals(Biome.COLD_BEACH) || biome.equals(Biome.STONE_BEACH)){
                    isWaterBiome = true; //aquanether is fine anywhere, no bridging relevant
                }

                if (!isWaterBiome && !isTagged && isBastionMature){
                    return; //no restrictions wheee
                } else {
                    moreInfo = true;
                }
            }
        
        Location differenceVector = event.getBlockPlaced().getLocation().subtract(event.getBlockAgainst().getLocation());
		if (event.getBlockAgainst().getType() == Material.BIRCH_FENCE
                && differenceVector.getBlockX() == 0
                && differenceVector.getBlockZ() == 0
                && differenceVector.getBlockY() == 1){
			return;
		}
		if (!event.getPlayer().getLocation().add(0, -0.01, 0).getBlock().getType().isSolid() || event.getPlayer()
				.getLocation().add(0, -0.01, 0).getBlock().getLocation().equals(event.getBlock().getLocation())) {
			if (event.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
				event.getPlayer().sendMessage("You must be standing on something solid. Use a bastion field to build freely.");
                if (moreInfo){
                    event.getPlayer().sendMessage("Bastion fields do not help if they are immature (<24 hours), you are in combat, or you are in a water biome.");
                }
				event.setCancelled(true);
                mostRecentCancel = event.getBlock();
                mostRecentCancelType = event.getBlock().getType();
                timeSinceLastCancel = System.currentTimeMillis();
			}
		} else {
            float yaw = event.getPlayer().getLocation().getYaw(); //starts at 0 south, becomes negative counter clockwise to -360 OR positive clockwise
            int blockX = event.getBlockPlaced().getX();
            int blockZ = event.getBlockPlaced().getZ();
            int playerX = event.getPlayer().getLocation().getBlockX();
            int playerZ = event.getPlayer().getLocation().getBlockZ();
            //LOG.info("BLOCKX " + blockX + " BLOCKZ " + blockZ + " PLAYERX " + playerX + " PLAYERZ " + playerZ + " YAW: " + yaw);
            if((Math.cos(Math.toRadians(yaw)) < 0 && blockZ > playerZ)
                    || (Math.cos(Math.toRadians(yaw)) > 0 && blockZ < playerZ)
                    || (Math.sin(Math.toRadians(yaw)) > 0 && blockX > playerX)
                    || (Math.sin(Math.toRadians(yaw)) < 0 && blockX < playerX)){
                event.getPlayer().sendMessage("You cannot place blocks behind you.");
                if (moreInfo){
                    event.getPlayer().sendMessage("Bastion fields do not help if they are immature, you are combat tagged, or you are in a water biome.");
                }
				event.setCancelled(true);
                mostRecentCancel = event.getBlock();
                mostRecentCancelType = event.getBlock().getType();
                timeSinceLastCancel = System.currentTimeMillis();
            }
        }
	}
    
    @EventHandler
	public void ghostBlocks(PlayerMoveEvent event) {
        double yCoordFrom = event.getFrom().getY();
        if (event.getTo().getY() > yCoordFrom //went higher (jump or climb or whatever, not run)
                && yCoordFrom - (int)yCoordFrom < 0.2 //first 1/5th of a block to reduce spam and help ensure they're actually leaping from that ghost SURFACE
                && System.currentTimeMillis() - timeSinceLastCancel < 200 //has been less than half a second since a block cancellation
                && (event.getFrom().getBlock().getRelative(0,-1,0).equals(mostRecentCancel) || event.getFrom().getBlock().equals(mostRecentCancel))){ //the block cancelled was the one right beneath the guy
            LOG.info("CRG: Player " + event.getPlayer().getDisplayName() + " possible ghost blocking. Start: " + event.getFrom().getBlock().getType().name() + 
                    " Underneath: " + event.getFrom().getBlock().getRelative(0,-1,0).getType().name() + " item in hand: " + event.getPlayer().getInventory().getItemInMainHand().getType().name() + 
                    " blockCancelled: " + mostRecentCancelType.name() + " fraction: " + (yCoordFrom - (int)yCoordFrom));
            for (Player p : event.getPlayer().getWorld().getPlayers()){
                if (p.isOp()){
                    p.sendMessage(ChatColor.RED + "Possible Ghost Blocks: " + ChatColor.WHITE + event.getPlayer().getDisplayName() + " -- See logs for more info.");
                }
            }
        }
    }
    
	@EventHandler
	public void airMeterChange(EntityAirChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player p = (Player) event.getEntity();
			int eventAirTicks = event.getAmount();
			int maxAir = (p.getMaximumAir());
			if (eventAirTicks == maxAir) { // refilling
				Location loc = p.getLocation();
				if (!loc.equals(null) && (loc.getY() > 253
						|| loc.getWorld().getBlockAt(loc.add(0, 1.62, 0)).getType().equals(Material.AIR))) {
					return; // head is in air
				} else {
					event.setCancelled(true);
				}
			}
		}
	}
    
    class DelayedChatAdd extends BukkitRunnable {
        Player player;

        public DelayedChatAdd(Player player){
            this.player = player;
        }
        @Override
        public void run(){
            player.chat("/nljg ! " + plugin.getConfig().getString("globalChatPassword")); // everything in august needs manual adding. THIS IS FUCKING NULL AGAIN GODDAMN THIS SHIT
            player.chat("/nljg ? " + plugin.getConfig().getString("globalChatPassword")); //needs full class with params and fucking goddammit. 
            player.chat("/gc !");
        }
    }
    
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
        postJoinTellCooldown.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        event.getPlayer().sendMessage(ChatColor.AQUA + "Joined world: " + event.getPlayer().getWorld().getName());
        if (System.currentTimeMillis() - event.getPlayer().getFirstPlayed() < 86400){
            event.getPlayer().sendMessage(ChatColor.AQUA + "Automatically adding you to global chat groups. '!' is general chat, '?' is for questions and help. '/g [group name]' will change your channel, and '/g' by itsel will put you in local nearby range only chat.");
            new DelayedChatAdd(event.getPlayer()).runTaskLater(plugin, 300);
        }
        if (plugin.ottTeleportMemory.containsKey(event.getPlayer().getDisplayName())){
            plugin.setPostOTTInventory(event.getPlayer());
            event.getPlayer().teleport(plugin.ottTeleportMemory.get(event.getPlayer().getDisplayName()));
            return;
        }
        if (!event.getPlayer().hasPlayedBefore() && !plugin.ottTeleportMemory.containsKey(event.getPlayer().getDisplayName())) {
			if (!event.getPlayer().getWorld().getName().contains("world") || event.getPlayer().getWorld().getName().contains("new_world")) {
				return;
			}
			World w = event.getPlayer().getWorld();
			//ItemStack[] kit = null;
			giveKit(event.getPlayer(), event.getPlayer().getLocation().getBlock().getBiome());
			spawnPick(w,event.getPlayer());
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.isOp()) {
					p.sendMessage(event.getPlayer().getDisplayName() + " is a new player");
				}
			}
		}
        if(bubbleMemory.containsKey(event.getPlayer().getUniqueId().toString())){
            event.getPlayer().setRemainingAir(bubbleMemory.get(event.getPlayer().getUniqueId().toString()));
        }
	}
    
	@EventHandler
	public void spawnmove(PlayerRespawnEvent event) {
		LOG.info("ONRESPAWN ==== " + event.getPlayer().getWorld().getName());
		this.plugin.getLogger().info(event.getRespawnLocation().getWorld().getName());
		if (event.getRespawnLocation().getWorld().getName().contains("ocean")
                || event.getRespawnLocation().getWorld().getName().contains("Civrealms_Lobby")
                || event.getRespawnLocation().getWorld().getName().contains("aqua")) {
			return;
		}
		World w = event.getRespawnLocation().getWorld();
        Location bedLoc = findProperBedSpawnLocation(event.getPlayer(),event.getPlayer().getBedSpawnLocation());
        if (bedLoc == null) {
			giveKit(event.getPlayer(), event.getPlayer().getLocation().getBlock().getBiome());
			spawnPick(w,event.getPlayer());
		} else {
            if (this.bedMode.containsKey(event.getPlayer().getUniqueId().toString())) { // if so, then bed spawned since last restart
				// check if mode is random or not random (value of hashmap)
				// if random, randomspawn. if not, do nothing except to set the randomspawn mode
				// to true.
				if (this.bedMode.get(event.getPlayer().getUniqueId().toString()).equals(Boolean.FALSE)) {
					giveKit(event.getPlayer(), event.getPlayer().getLocation().getBlock().getBiome());
					spawnPick(w,event.getPlayer());
					event.getPlayer()
							.sendMessage(ChatColor.DARK_RED + "You have randomspawned to discourage bed kill camping.");
					event.setRespawnLocation(event.getPlayer().getBedSpawnLocation());
				} else {
                    delayedTeleport(event.getPlayer(),bedLoc);
					event.getPlayer()
							.sendMessage(ChatColor.RED + "If you die again before clicking the mouse, you will randomspawn.");
				}
			} else {
                delayedTeleport(event.getPlayer(),bedLoc);
				event.getPlayer()
						.sendMessage(ChatColor.RED + "If you die again before clicking the mouse, you will randomspawn.");
			}
			this.bedMode.put(event.getPlayer().getUniqueId().toString(), Boolean.FALSE); // bed mode false, i.e. random
																							// mode
		}
	}
    
	private void spawnPick(World w, Player p) {
		int xspwn = (int) ((Math.random() * plugin.getConfig().getInt("spawnWidth"))
				+ plugin.getConfig().getInt("spawnXcenter") - (plugin.getConfig().getInt("spawnWidth") / 2));
		int zspwn = (int) ((Math.random() * plugin.getConfig().getInt("spawnHeight"))
				+ plugin.getConfig().getInt("spawnZcenter") - (plugin.getConfig().getInt("spawnHeight") / 2));

        Block highestBlock = SekkritClass.getHighestCustomForRespawn(xspwn, zspwn, w);
        while (highestBlock == null){
            xspwn = (int) ((Math.random() * plugin.getConfig().getInt("spawnWidth"))
				+ plugin.getConfig().getInt("spawnXcenter") - (plugin.getConfig().getInt("spawnWidth") / 2));
            zspwn = (int) ((Math.random() * plugin.getConfig().getInt("spawnHeight"))
				+ plugin.getConfig().getInt("spawnZcenter") - (plugin.getConfig().getInt("spawnHeight") / 2));
            highestBlock = SekkritClass.getHighestCustomForRespawn(xspwn, zspwn, w);
        }
        int highestBlockY = highestBlock.getY();
		delayedTeleport(p,new Location(w,xspwn+0.5, highestBlockY+1, zspwn+0.5));
        w.setSpawnLocation(xspwn, highestBlockY+1, zspwn);
	}
        
    @EventHandler
    public void onIslandWaterPolicyPistons(BlockPistonExtendEvent event){
        if (plugin.getConfig().getString("worldName").equals("CivRealms_taiga_island")
                || plugin.getConfig().getString("worldName").contains("new_world")){
            LOG.info("Piston island check at " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ());
            for(Block b : event.getBlocks()){
                if (b.getBiome() == Biome.DEEP_OCEAN || (plugin.getConfig().getString("worldName").equals("CivRealms_taiga_island") && b.getBiome() == Biome.OCEAN)){
                    event.setCancelled(true);
                    return;
                }
                if (b.getType() == Material.SLIME_BLOCK){
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onIslandWaterPolicySponges(BlockPlaceEvent event){
        if (plugin.getConfig().getString("worldName").equals("CivRealms_taiga_island")
                || plugin.getConfig().getString("worldName").contains("new_world")){
            if (event.getBlock().getType() == Material.SPONGE && (event.getBlock().getBiome() == Biome.OCEAN || event.getBlock().getBiome() == Biome.DEEP_OCEAN)){
                event.getBlock().setData((byte)1);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
	public void onEnderFramePlace(BlockPlaceEvent event) {
        if (event.getPlayer().isOp() && event.getPlayer().isSneaking()){
            return;
        }
        boolean damageCooledDown = true;
        UUID uid = event.getPlayer().getUniqueId();
        if ((enderFrameCooldown.containsKey(uid) && (System.currentTimeMillis() - enderFrameCooldown.get(uid)) < 2000)
                || (nonEnderFrameCooldown.containsKey(uid) && (System.currentTimeMillis() - nonEnderFrameCooldown.get(uid)) < 2000)){
            damageCooledDown = false;
        }
        
        if (event.getBlock().getType() == Material.ENDER_PORTAL_FRAME){
            if(damageCooledDown){
                enderFrameCooldown.put(uid, System.currentTimeMillis());
                Set<BastionBlock> blocking = com.civrealms.crgmain.BastionChecks.getBastions(event.getBlock());
                if (blocking.size() > 0){
                    for (BastionBlock bb : blocking){
                        bb.erode(bb.getErosionFromBlock());
                    }
                }
            }
            event.setCancelled(true); // always cancel portal frames no matter what
        } else {
            if (enderFrameCooldown.containsKey(uid) && (System.currentTimeMillis() - enderFrameCooldown.get(uid)) < 2000){ 
                //if you have used a frame less than 2 seconds ago, cancel normal block place, and give error
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Placements canceled within 2s of portal frames doing damage.");
            } else {
                nonEnderFrameCooldown.put(uid, System.currentTimeMillis()); //normally, don't cancel but do log it for latrer if using frame
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
	public void onEnderFrameQuery(PlayerInteractEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand() != null
                && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.ENDER_PORTAL_FRAME
                && event.getAction() == Action.LEFT_CLICK_BLOCK
                && event.getHand() == EquipmentSlot.HAND){
            Set<BastionBlock> blocking = com.civrealms.crgmain.BastionChecks.getBastions(event.getClickedBlock());
            if (blocking.size() > 0){
                for (BastionBlock bb : blocking){
                    Location loc = bb.getLocation();
                    if (bb.canPlace(event.getPlayer())){
                        event.getPlayer().sendMessage(ChatColor.DARK_GREEN + "Friendly bastion at " + loc.getBlockX() + ", " + loc.getY() + ", " + loc.getZ() + "." );
                    } else {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "Unfriendly bastion at " + loc.getBlockX() + ", " + loc.getY() + ", " + loc.getZ() + "." );
                    }
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void undoSprint(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && ((Player)event.getEntered()).isSprinting()){
            ((Player)event.getEntered()).setSprinting(false);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void newWorldCropPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Block block = event.getBlock();
        if (block.getWorld().getName().contains("new_world")){
            if (SekkritClass.checkLaurel(block)){
                //dark oak in laurel land
                if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("Laurel")){
                    return;
                } else {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot place that species of log here.");
                    return;
                }
            } else if (SekkritClass.checkTea(block)){
                //grass in tea land
                if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("harbors")){
                    return;
                } else {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot plant that species here.");
                    return;
                }
            } else if (SekkritClass.checkCoffee(block)){
                //cocoa in coffee land
                if (item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("Bitter")){
                    return;
                } else {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot plant that species here.");
                    return;
                }
            }
        }
        if (block.getType() == Material.COCOA && item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("Bitter")
            && !(SekkritClass.checkCoffee(block))){
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot plant that species here.");
            return;
        } else if (block.getType() == Material.WATER_LILY && item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("harbors")){
            //tea in non-tea land
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot plant that species here.");
            return;
        } else if (block.getType() == Material.LOG_2 && (block.getData() == 1 || block.getData() == 5 || block.getData() == 9)
                && item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).contains("Laurel")){
            //laurel in not-laurel land
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot plant that species here.");
            return;
        }
    }
        
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onShearAttempt(PlayerInteractEntityEvent event) {
        if (((event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.SHEARS)
                || (event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHEARS))
                && event.getRightClicked() != null && event.getRightClicked().getType() == EntityType.SHEEP){
            Entity sheep = event.getRightClicked();
            if (sheep != null && sheep instanceof Sheep 
                    && sheepSheared.containsKey(sheep.getUniqueId().toString()) 
                    && System.currentTimeMillis() - sheepSheared.get(sheep.getUniqueId().toString()) > 100 
                    && ((Sheep)sheep).isSheared()){
                ((Sheep)sheep).damage(1);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onShearAttempt(PlayerShearEntityEvent event) {
        sheepSheared.put(event.getEntity().getUniqueId().toString(), System.currentTimeMillis());
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onCactusClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CACTUS &&
                ((event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.STICK)
                || (event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType() == Material.STICK)
                || (event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType() == Material.BONE)
                || (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BONE))){
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void voidFeatherFall(PlayerMoveEvent event) {
        if(event.getTo().getY()<1){
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,100,253));
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onLeafSnipe(PlayerInteractEvent event) {
        if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.SHEARS && event.getPlayer().isSneaking() && event.getAction().name().equals("RIGHT_CLICK_AIR") && event.getHand() == EquipmentSlot.HAND){
            List<Block> lineOfSight = event.getPlayer().getLineOfSight(null, 100);
            Block firstBlock = lineOfSight.get(0);
            for (Block b : lineOfSight){
                if (b.getType() != Material.AIR){
                    firstBlock = b;
                    break;
                }
            }
            Material firstBlockType = firstBlock.getType();
            if (firstBlockType == Material.LEAVES || firstBlockType == Material.LEAVES_2 || firstBlockType == Material.LOG || firstBlockType == Material.LOG_2){
                if (!leafSnipeCooldown.containsKey(event.getPlayer().getUniqueId().toString()) || System.currentTimeMillis() - leafSnipeCooldown.get(event.getPlayer().getUniqueId().toString()) > 15000){
                    //bastion check
                    
                    Set<BastionBlock> blocking = com.civrealms.crgmain.BastionChecks.getBastions(firstBlock);
                    if (blocking.size() > 0){
                        for (BastionBlock bb : blocking){
                            if (!bb.canPlace(event.getPlayer())){
                                return;
                            }
                        }
                        int leafCount = 0;
                        int logCount = 0;
                        leafSnipeCooldown.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
                        LOG.info("Leaf Snipe Event: " + event.getPlayer().getDisplayName() + " sniped at: " + firstBlock.getX() + "," + firstBlock.getY() + "," + firstBlock.getZ());
                        for (int checkX = -5; checkX < 6; checkX++){
                            for (int checkZ = -5; checkZ < 6; checkZ++){
                                for (int checkY = -5; checkY < 6; checkY++){
                                    Block checkBlock = firstBlock.getRelative(checkX,checkY,checkZ);
                                    if ((checkBlock.getType() == Material.LEAVES || checkBlock.getType() == Material.LEAVES_2) && leafCount < 40 && !com.civrealms.crgmain.CitadelChecks.isReinforced(checkBlock, event.getPlayer())){
                                        checkBlock.setType(Material.AIR);
                                        leafCount++;
                                    } else if((checkBlock.getType() == Material.LOG || checkBlock.getType() == Material.LOG_2) && logCount < 8 && !com.civrealms.crgmain.CitadelChecks.isReinforced(checkBlock, event.getPlayer())){
                                        checkBlock.setType(Material.AIR);
                                        logCount++;
                                    } else if((checkBlock.getType() == Material.PUMPKIN || checkBlock.getType().name().contains("FENCE")) && logCount < 8 && !com.civrealms.crgmain.CitadelChecks.isReinforced(checkBlock, event.getPlayer())){
                                        checkBlock.setType(Material.AIR);
                                        logCount++;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "You can only leaf snipe every 60 seconds.");
                    return;
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void OnPrisonDirtTools(PlayerInteractEvent event) {
		if (event.getAction().name().equals("RIGHT_CLICK_BLOCK") && event.getPlayer().getWorld().getName().equals("prison_the_end") && event.getHand() == EquipmentSlot.HAND){
            if (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType().name().contains("HOE")
                || event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType().name().contains("HOE")){
                Block b = event.getClickedBlock();
                if (b.getType() == Material.DIRT && !com.civrealms.crgmain.CitadelChecks.isReinforced(b,event.getPlayer())){
                    b.setType(Material.SOIL);
                } else if (b.getType() == Material.GRASS && !com.civrealms.crgmain.CitadelChecks.isReinforced(b,event.getPlayer())){
                    b.setType(Material.DIRT);
                }
            } else if (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType().name().contains("HOE")
                || event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType().name().contains("HOE")){
                Block b = event.getClickedBlock();
                if (b.getType() == Material.GRASS && !com.civrealms.crgmain.CitadelChecks.isReinforced(b,event.getPlayer())){
                    b.setType(Material.GRASS_PATH);
                }
            }
        }
    }
    
	

	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onBedMode(PlayerInteractEvent event) {
		if (this.bedMode.containsKey(event.getPlayer().getUniqueId().toString())) {
			if (this.bedMode.get(event.getPlayer().getUniqueId().toString()).equals(Boolean.FALSE)) {
				this.bedMode.put(event.getPlayer().getUniqueId().toString(), Boolean.TRUE);
				event.getPlayer().sendMessage(ChatColor.AQUA + "Bed spawn mode enabled.");
			}
		}
	}
    
	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onBucketBreath(PlayerInteractEvent event) {
		if (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BUCKET)
                || event.getPlayer().getInventory().getItemInOffHand() != null && event.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.BUCKET)) {
			if (event.getPlayer().getLocation().add(0, 1, 0).getBlock().getType().equals(Material.WATER) || event
					.getPlayer().getLocation().add(0, 1, 0).getBlock().getType().equals(Material.STATIONARY_WATER)) {
				event.getPlayer().sendMessage("You cannot fill buckets while your head is submerged.");
				event.setCancelled(true);
			}
		}
	}
    
    //@EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
	//public void onRecordJuke(PlayerInteractEvent event) {
	//	if (event.getAction().name().equals("RIGHT_CLICK_BLOCK") && event.getClickedBlock().getType() == Material.JUKEBOX){
    //        event.setCancelled(false);
    //    }
	//}
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
	public void onEmeraldBreak(PlayerInteractEvent event) {
        Material mainHand = event.getPlayer().getInventory().getItemInMainHand().getType();
		if (event.getHand() == EquipmentSlot.HAND 
                && (event.getAction().name().equals("RIGHT_CLICK_BLOCK") || event.getAction().name().equals("RIGHT_CLICK_AIR")) && mainHand == Material.EMERALD){
            event.setCancelled(false);
            event.getPlayer().getInventory().getItemInMainHand().setAmount(event.getPlayer().getInventory().getItemInMainHand().getAmount()-1);
            event.getPlayer().giveExp(90);
        }
	}

	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onSports(PlayerInteractEvent event) {
		if (event.hasBlock()) {
            Material tool = event.getPlayer().getInventory().getItemInMainHand().getType();
			if (tool == Material.WOOD_HOE) {
				if (event.getClickedBlock().getType() == Material.SPONGE) {
					if (event.getAction().name().equals("RIGHT_CLICK_BLOCK") && event.getClickedBlock()
							.getRelative(event.getBlockFace()).getType().equals(Material.AIR) && event.getHand() == EquipmentSlot.HAND) {
						boolean playingField = false;
						Block destination = event.getClickedBlock().getRelative(event.getBlockFace());
						for (int i = 1; i < plugin.sportsHeight; i++) {
							if (destination.getRelative(0, -i, 0).getType().equals(Material.IRON_BLOCK)) {
								playingField = true;
								break;
							}
						}
						if (playingField) {
                            byte wetness = event.getClickedBlock().getData();
							event.getClickedBlock().setType(Material.AIR);
							event.getClickedBlock().getRelative(event.getBlockFace()).setType(Material.SPONGE);
                            event.getClickedBlock().getRelative(event.getBlockFace()).setData(wetness);
						}
					} else if (event.getAction().name().equals("LEFT_CLICK_BLOCK") && event.getClickedBlock()
							.getRelative(event.getBlockFace().getOppositeFace()).getType().equals(Material.AIR) && event.getHand() == EquipmentSlot.HAND) {
						boolean playingField = false;
						Block destination = event.getClickedBlock().getRelative(event.getBlockFace().getOppositeFace());
						for (int i = 1; i < plugin.sportsHeight; i++) {
							if (destination.getRelative(0, -i, 0).getType().equals(Material.IRON_BLOCK)) {
								playingField = true;
								break;
							}
						}
						if (playingField) {
                            byte wetness = event.getClickedBlock().getData();
							event.getClickedBlock().setType(Material.AIR);
							event.getClickedBlock().getRelative(event.getBlockFace().getOppositeFace())
									.setType(Material.SPONGE);
                            event.getClickedBlock().getRelative(event.getBlockFace()).setData(wetness);
						}
					}
				} else {
					event.setCancelled(true); // no tilling fields with the spleef hook!
				}
			}
		}
	}
    
    @EventHandler(ignoreCancelled = false,priority = EventPriority.MONITOR)
    public void onPrisonBed(BlockPlaceEvent event){
        Player p = event.getPlayer();
        if(p.getWorld().getName().equals("prison_the_end") && event.getBlock().getType() == Material.BED_BLOCK){
            event.setCancelled(true);
            if(event.getBlockReplacedState().getType() == Material.AIR){
                event.setCancelled(false);
            }
        }
    }
    
    private Location getFootOfBedLoc(Block b){
        Bed theBed = ((Bed)(b.getState().getData()));
        BlockFace headFacing = theBed.getFacing();
        Block beforeFootOfBed = null;
        if(theBed.isHeadOfBed()){
            if(headFacing == BlockFace.NORTH){
                beforeFootOfBed = b.getRelative(0,0,1);
            } else if(headFacing == BlockFace.SOUTH){
                beforeFootOfBed = b.getRelative(0,0,-1);
            } else if(headFacing == BlockFace.EAST){
                beforeFootOfBed = b.getRelative(-1,0,0);
            } else {
                beforeFootOfBed = b.getRelative(1,0,0);
            }
        } else {
            beforeFootOfBed = b.getRelative(0,0,0);
        }
        return beforeFootOfBed.getLocation().add(0.5, 0, 0.5);
    }
    
    public void setBedSpawn(Player p, Location loc){
        BukkitRunnable r = new BukkitRunnable() {
            public void run() {
                p.setBedSpawnLocation(loc,true);
                LOG.info("bed spawn set at: " + loc.getBlockX() + ", " + loc.getBlockZ());
            }
        };
        r.runTaskLater(plugin, 2);
    }
    
    public void delayedTeleport(Player p, Location loc){
        BukkitRunnable r = new BukkitRunnable() {
            public void run() {
                p.teleport(loc);
            }
        };
        r.runTaskLater(plugin, 2);
    }
    
    public Location findProperBedSpawnLocation(Player p, Location loc){
		if (loc == null){
            return null;
        }
        Block playerBlock = loc.getBlock();
        if (playerBlock.getType() == Material.BED_BLOCK){
            Material aboveFoot = getFootOfBedLoc(loc.getBlock()).getBlock().getRelative(0,1,0).getType();
            if (aboveFoot == Material.AIR){
                return loc;
            } else {
                p.sendMessage(ChatColor.RED + "The foot of your bed needs air above it. Bed is obstructed.");
                return null;
            }
        }
        for (int x = -1; x < 2; x++){
            for (int y = -1; y < 2; y++){
                for (int z = -1; z < 2; z++){
                    //p.sendMessage(playerBlock.getRelative(x,y,z).getType().name());
                    if (playerBlock.getRelative(x,y,z).getType() == Material.BED_BLOCK){
                        Material aboveFoot = getFootOfBedLoc(playerBlock.getRelative(x,y,z)).getBlock().getRelative(0,1,0).getType();
                        if (aboveFoot == Material.AIR){
                            return playerBlock.getRelative(x,y,z).getLocation().add(0.5,0,0.5);
                        } else {
                            p.sendMessage(ChatColor.RED + "The foot of your bed needs air above it. Bed is obstructed.");
                            return null;
                        } 
                    }
                }
            }
        }
        for (int x = -2; x < 3; x++){
            for (int y = -1; y < 2; y++){
                for (int z = -2; z < 3; z++){
                    //p.sendMessage(playerBlock.getRelative(x,y,z).getType().name());
                    if (playerBlock.getRelative(x,y,z).getType() == Material.BED_BLOCK){
                        Material aboveFoot = getFootOfBedLoc(playerBlock.getRelative(x,y,z)).getBlock().getRelative(0,1,0).getType();
                        if (aboveFoot == Material.AIR){
                            return playerBlock.getRelative(x,y,z).getLocation().add(0.5,0,0.5);
                        } else {
                            p.sendMessage(ChatColor.RED + "The foot of your bed needs air above it. Bed is obstructed.");
                            return null;
                        } 
                    }
                }
            }
        }
        for (int x = -3; x < 4; x++){
            for (int y = -2; y < 3; y++){
                for (int z = -3; z < 4; z++){
                    //p.sendMessage(playerBlock.getRelative(x,y,z).getType().name());
                    if (playerBlock.getRelative(x,y,z).getType() == Material.BED_BLOCK){
                        Material aboveFoot = getFootOfBedLoc(playerBlock.getRelative(x,y,z)).getBlock().getRelative(0,1,0).getType();
                        if (aboveFoot == Material.AIR){
                            //p.sendMessage("new crg bed loc x:" + playerBlock.getRelative(x,y,z).getLocation().add(0.5,0,0.5).getX() + ", Z:" + playerBlock.getRelative(x,y,z).getLocation().add(0.5,0,0.5).getZ());
                            return playerBlock.getRelative(x,y,z).getLocation().add(0.5,0,0.5);
                        } else {
                            p.sendMessage(ChatColor.RED + "The foot of your bed needs air above it. Bed is obstructed.");
                            return null;
                        }
                    }
                }
            }
        }
        //p.sendMessage("reached end");
        return null;
    }
    
    @EventHandler (ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBedClick(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.BED_BLOCK && event.getHand() == EquipmentSlot.HAND){
            setBedSpawn(event.getPlayer(),getFootOfBedLoc(event.getClickedBlock()));
            if (event.getPlayer().getWorld().getName().equals("prison_the_end")){
                event.getPlayer().sendMessage(ChatColor.AQUA + "Bed Spawn Set.");
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBedLeave(PlayerBedLeaveEvent event) {
            event.getPlayer().teleport(getFootOfBedLoc(event.getBed()));
            setBedSpawn(event.getPlayer(),getFootOfBedLoc(event.getBed()));
    }

	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onElevator(PlayerInteractEvent event) {
		if (event.hasBlock() && event.getHand() == EquipmentSlot.HAND) {
			// Gold teleport block
			if (event.getClickedBlock().getType() == Material.GOLD_BLOCK) {
				Location loc = event.getPlayer().getLocation();
				loc.add(0, -1, 0);
				if (event.getClickedBlock().getLocation().distanceSquared(loc) < 1.0) {

					int airCount = 0;
					int relativeAltitude = 1;
					boolean pocketFound = false;
					boolean goldFound = false;
                    Material itemInHand = event.getPlayer().getInventory().getItemInMainHand().getType();
					if (event.getAction().name() == "RIGHT_CLICK_BLOCK" && itemInHand != Material.TORCH && itemInHand != Material.SIGN) {
						while (!pocketFound && relativeAltitude + event.getClickedBlock().getY() < 256) {
							relativeAltitude++;
                            Material toCheck = event.getClickedBlock().getRelative(0, relativeAltitude, 0).getType();
							if (toCheck == Material.AIR || toCheck == Material.SIGN || toCheck == Material.SIGN_POST || toCheck == Material.WALL_SIGN) {
								airCount++;
								if (goldFound && airCount > 1) {
									loc.setY(event.getClickedBlock().getY() + relativeAltitude - 1);
                                    loc.setX(event.getClickedBlock().getX() + 0.5);
                                    loc.setZ(event.getClickedBlock().getZ() + 0.5);
									pocketFound = true;
								}
							} else {
								airCount = 0;
								goldFound = false;
								if (event.getClickedBlock().getRelative(0, relativeAltitude, 0)
										.getType() == Material.GOLD_BLOCK) {
									goldFound = true;
								}
							}
						}
						if (relativeAltitude + event.getClickedBlock().getY() > 255) {
							return;
						}
					} else { // LEFT CLICK
						while (!pocketFound && relativeAltitude + event.getClickedBlock().getY() > 0) {
							relativeAltitude--;
                            Material toCheck = event.getClickedBlock().getRelative(0, relativeAltitude, 0).getType();
							if (toCheck == Material.AIR || toCheck == Material.SIGN || toCheck == Material.SIGN_POST || toCheck == Material.WALL_SIGN) {
								airCount++;
							} else {
								goldFound = false;
								if (event.getClickedBlock().getRelative(0, relativeAltitude, 0)
										.getType() == Material.GOLD_BLOCK) {
									goldFound = true;
								}
								if (goldFound && airCount > 1) {
									loc.setY(event.getClickedBlock().getY() + relativeAltitude + 1);
                                    loc.setX(event.getClickedBlock().getX() + 0.5);
                                    loc.setZ(event.getClickedBlock().getZ() + 0.5);
									pocketFound = true;
								}
								airCount = 0;
							}
						}
						if (relativeAltitude + event.getClickedBlock().getY() < 1) {
							return;
						}
					}
					event.getPlayer().teleport(loc);
					event.getPlayer().setVelocity(new Vector(0, 0, 0));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void fixDurabilty(PlayerItemDamageEvent event) {
        Material type = event.getItem().getType();
        //attempt to make it say "too expensive!" by default, but doesn't work:
        //if (event.getItem().hasItemMeta() && event.getItem().getItemMeta() instanceof Repairable){
        //    ((Repairable) event.getItem().getItemMeta()).setRepairCost(10000);
        //}
        if ((type == Material.DIAMOND_PICKAXE || type == Material.DIAMOND_SPADE || type == Material.DIAMOND_AXE) && event.getItem().getDurability() > event.getItem().getType().getMaxDurability()-6
                && event.getItem().getDurability() != event.getItem().getType().getMaxDurability()-6
                && event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasEnchants()){
            if (type == Material.DIAMOND_PICKAXE){
                event.getPlayer().sendMessage(ChatColor.RED + "Your pick is almost broken.");
            } else if (type == Material.DIAMOND_SPADE) {
                event.getPlayer().sendMessage(ChatColor.RED + "Your shovel is almost broken.");
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "Your axe is almost broken.");
            }
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,40,2));
        }
        if (event.getItem().hasItemMeta()) {
			if (event.getItem().getItemMeta().hasLore()) {
				// List<String> il = event.getItem().getItemMeta().getLore();
				if (event.getItem().getType() == (Material.GOLD_PICKAXE)
						|| type == (Material.GOLD_SPADE)
						|| type == (Material.GOLD_AXE)
						|| type == (Material.GOLD_SWORD)
						|| type == (Material.GOLD_HOE)
                        || type == (Material.GOLD_BOOTS)
                        || type == (Material.GOLD_LEGGINGS)
                        || type == (Material.GOLD_CHESTPLATE)
                        || type == (Material.GOLD_HELMET)
                        || type == (Material.GOLD_BARDING)) {
					if (Math.random() > plugin.goldDuraHitChance) {
						event.setCancelled(true);
					}
				} else if (type == (Material.IRON_PICKAXE) // IRON OR STEEL, steel comes with
						|| type == (Material.IRON_SPADE)
						|| type == (Material.IRON_AXE)
						|| type == (Material.IRON_SWORD)
						|| type == (Material.IRON_HOE)
                        || type == (Material.IRON_HOE)
                        || type == (Material.IRON_BOOTS)
                        || type == (Material.IRON_LEGGINGS)
                        || type == (Material.IRON_CHESTPLATE)
                        || type == (Material.IRON_HELMET)
                        || type == (Material.IRON_BARDING)
                        || type == (Material.CHAINMAIL_BOOTS)
                        || type == (Material.CHAINMAIL_LEGGINGS)
                        || type == (Material.CHAINMAIL_CHESTPLATE)
                        || type == (Material.CHAINMAIL_HELMET)) {
					if (Math.random() > plugin.ironSteelDuraHitChance) {
						event.setCancelled(true);
					}
				}
			}
		}
        String typeName = type.name();
        if(typeName.contains("HELMET") || typeName.contains("CHESTPLATE") || typeName.contains("LEGGINGS") || typeName.contains("BOOTS")){
            Player p = event.getPlayer();
            int originalDamage = event.getDamage();
            int damageToApply = 3;
            if(lastDamageType.containsKey(p.getUniqueId().toString()) && lastDamageType.get(p.getUniqueId().toString()) == "PROJECTILE"){
                damageToApply = Math.max((int)(((lastDamageAmount.get(p.getUniqueId().toString())+0.75)/8.0)*9.0),3); // minimum 3 dura, but up to 9 for a point blank full draw hit
            } 
            int duraLevel = 0;
            if(event.getItem().containsEnchantment(Enchantment.DURABILITY)){
                duraLevel = event.getItem().getEnchantmentLevel(Enchantment.DURABILITY);
            }
            if(event.getItem().getType() == Material.DIAMOND_CHESTPLATE && event.getItem().containsEnchantment(Enchantment.THORNS)){
                damageToApply = damageToApply + 2*(event.getItem().getEnchantmentLevel(Enchantment.THORNS));
            }
            if(Math.ceil(Math.random()*(duraLevel+1)) > 1){
                damageToApply = 0;
            }
            //LOG.info(typeName + " dura intended: " + damageToApply);
            event.getItem().setDurability((short)Math.min(event.getItem().getDurability()-originalDamage+damageToApply,event.getItem().getType().getMaxDurability()));
        }
	}
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState() && Math.random()<0.2){
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void recordDamageType(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player){
            this.lastDamageType.put(((Player)event.getEntity()).getUniqueId().toString(),event.getCause().name());
            this.lastDamageAmount.put(((Player)event.getEntity()).getUniqueId().toString(),event.getDamage());
        }
    }
    
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPearlTeleport(PlayerTeleportEvent event) {
		TeleportCause cause = event.getCause();
		if (cause.equals(TeleportCause.ENDER_PEARL)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("This item is only used to exile players");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBedrockPiston(BlockPistonExtendEvent event) {
		if (event.getBlock().getY() < plugin.pistonBedrockExclusion || (plugin.getConfig().getString("worldName").contentEquals("aqua") && event.getBlock().getY() > 254 - plugin.pistonBedrockExclusion)) {
			event.getBlock().breakNaturally();
			event.getBlock().setType(Material.AIR);
			event.setCancelled(true);
            LOG.info("Piston popped at " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onScaffoldClimb(PlayerMoveEvent event) {
		if (event.getPlayer().getLocation().getBlock().getType() == Material.BIRCH_FENCE) {
			double pitchLook = event.getPlayer().getLocation().getPitch();
			if (pitchLook > 60 || event.getPlayer().isSneaking()) {
				event.getPlayer().setVelocity(new Vector(0, -0.2, 0));
			} else if (pitchLook < 60) {
				event.getPlayer().setVelocity(new Vector(0, 0.2, 0));
			}
			event.getPlayer().setFallDistance(1);
		}
	}
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void lilyPadCancel(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.WATER_LILY && event.getBlock().getY() > 1 && !event.getBlock().getRelative(0,-2,0).getType().isSolid()){
            event.setCancelled(true);
            mostRecentCancel = event.getBlock();
            mostRecentCancelType = event.getBlock().getType();
            timeSinceLastCancel = System.currentTimeMillis();
            LOG.info("Player " + event.getPlayer().getDisplayName() + " failed to place a lilypad. Possible bridge exploiting in large numbers.");
        }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void ChorusPlantingCancel(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.CHORUS_FLOWER){
            event.setCancelled(true);
        }
    }
        
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void bastionReinforce(ReinforcementCreationEvent event) {
        if (event.getBlock().getType() == Material.ENDER_CHEST && !vg.civcraft.mc.citadel.PlayerState.get(event.getPlayer()).getMode().equals(ReinforcementMode.REINFORCEMENT_FORTIFICATION)){
            event.getPlayer().sendMessage(ChatColor.RED + "Please only use /ctf to reinforce bastions.");
            event.setCancelled(true);
        }
    }
        
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void bastionReinforce(ReinforcementChangeTypeEvent event) {
        if (event.getReinforcement().getLocation().getBlock().getType() == Material.ENDER_CHEST){
            event.getPlayer().sendMessage(ChatColor.RED + "Please only use /ctf to reinforce bastions.");
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
	public void rememberAir(PlayerQuitEvent event) {
        bubbleMemory.put(event.getPlayer().getUniqueId().toString(), event.getPlayer().getRemainingAir());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
	public void noChangePhaseDragon(EnderDragonChangePhaseEvent event) {
        event.setCancelled(true);
    }
    
    public void noSpawnDragon(CreatureSpawnEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            event.setCancelled(true);
        }
    }
    
    public void noDragonCreatePortal(EntityCreatePortalEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void OnPearlThrowAttempt(PlayerInteractEvent event) {
        if (event.getAction().name().equals("RIGHT_CLICK_AIR") || event.getAction().name().equals("RIGHT_CLICK_BLOCK")){
            PlayerInventory i = event.getPlayer().getInventory();
            if ((i.getItemInMainHand() != null && i.getItemInMainHand().getType() == Material.ENDER_PEARL)
                    || (i.getItemInOffHand() != null && i.getItemInOffHand().getType() == Material.ENDER_PEARL)){
                event.setCancelled(true);
            }
        }
    }
    
    /*
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
	public void onEventLogToggle(PlayerInteractEvent event) {
        if (event.getAction().name().equals("RIGHT_CLICK_AIR") && event.getPlayer().isOp()
                && event.getPlayer().getInventory().getItemInOffHand() != null
                && event.getPlayer().getInventory().getItemInOffHand().getType() == Material.ANVIL){
            //enableGlobalListener();
            LOG.info("itemconsume:");
            for (RegisteredListener r : PlayerItemConsumeEvent.getHandlerList().getRegisteredListeners()){
                LOG.info(r.getPlugin().getName());
            }
            LOG.info("entity death:");
            for (RegisteredListener r : EntityDeathEvent.getHandlerList().getRegisteredListeners()){
                LOG.info(r.getPlugin().getName());
            }
            LOG.info("logout:");
            for (RegisteredListener r : PlayerQuitEvent.getHandlerList().getRegisteredListeners()){
                LOG.info(r.getPlugin().getName());
            }
            LOG.info("ede:");
            for (RegisteredListener r : EntityDamageEvent.getHandlerList().getRegisteredListeners()){
                LOG.info(r.getPlugin().getName());
            }
        }
    }
    
    private void enableGlobalListener() {
        RegisteredListener registeredListener = new RegisteredListener(this, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                if (!(event instanceof PlayerStatisticIncrementEvent
                        || event instanceof PlayerMoveEvent
                        || event instanceof ChunkLoadEvent
                        || event instanceof ChunkUnloadEvent
                        || event instanceof PlayerAnimationEvent
                        || event instanceof EntityDeathEvent
                        || event instanceof PlayerQuitEvent)) {
                    System.out.println(event.getEventName());
                }
            }
        }, EventPriority.MONITOR, plugin, false);
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.register(registeredListener);
        }
    }*/
}