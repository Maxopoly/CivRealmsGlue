package com.civrealms.crgpvp;

//java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;

//events
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

//other bukkit
import com.civrealms.crgmain.CivRealmsGlue;
import java.util.HashSet;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 *
 * @author Gavin
 */
public class PVPListener implements Listener {
    
	private CivRealmsGlue plugin;
	public static Logger LOG = Logger.getLogger("CivRealmsGlue");
    private long lastMiteSweep = 0;
    HashMap<UUID,Long> lastDamagedByEntity = new HashMap<UUID,Long>();
    HashMap<String,Long> lastJoined = new HashMap<String,Long>();
    HashMap<UUID,Long> AnimationMemory = new HashMap<UUID,Long>();
    double damageModifier = 2.0;
    private HashMap<UUID,Long> webCooldown = new HashMap<UUID,Long>();
    private HashSet<Material> foods = new HashSet<Material>();
    
	public PVPListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onGappleEat(PlayerItemConsumeEvent event){
        if (event.getItem().getType() == Material.GOLDEN_APPLE && event.getItem().getDurability() == 0){
            event.getPlayer().setCooldown(Material.GOLDEN_APPLE, 1200);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void PotionSplashEvent(PotionSplashEvent event) {
        PotionMeta pm = (PotionMeta)event.getPotion().getItem().getItemMeta();
        if (pm.getBasePotionData().getType().equals(PotionType.AWKWARD)
                || pm.getBasePotionData().getType().equals(PotionType.MUNDANE)
                || pm.getBasePotionData().getType().equals(PotionType.THICK)){
            LOG.info("awk/mun");
            for(LivingEntity le : event.getAffectedEntities()) {
                if (le instanceof Player){
                    LOG.info("should harm");
                    Player p = (Player)le;
                    double currHealthPercent = (p.getHealth()/p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                    p.setHealth(Math.max(currHealthPercent-0.2,0.1)*p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                }
            }
            return;
        }
        for(PotionEffect pe : event.getPotion().getEffects()) {
            //LOG.info(Integer.toString(pe.getType().getId()));
            //LOG.info(pe.getType().getName());
            //LOG.info(Integer.toString(pe.getAmplifier()));
            for(LivingEntity le : event.getAffectedEntities()) {
                if(pe.getType().equals(PotionEffectType.HARM) && le instanceof Player) {
                    continue;
                }
                if (pm.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL) && le instanceof Player && !le.isDead()){
                //if(pe.getType().getId() == 6 && le instanceof Player) {
                //if(pe.getType() == PotionEffectType.HEAL && le instanceof Player) {
                    event.setCancelled(true);
                    Player p = (Player)le;
                    double currHealthPercent = (p.getHealth()/p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                    if (pe.getAmplifier() == 0){
                        p.setHealth(Math.min(currHealthPercent+0.1,1.0)*p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                    } else if (pe.getAmplifier() == 1){
                        p.setHealth(Math.min(currHealthPercent+0.1,1.0)*p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                    }
                    continue;
                }
                le.removePotionEffect(pe.getType());
                le.addPotionEffect(pe);
            }
        }
        event.setCancelled(true);
    }
    
    public void setNoTicksAndLastDamage(LivingEntity ent, int ticks, double lastDamage){
        BukkitRunnable r = new BukkitRunnable() {
            public void run() {
                ent.setNoDamageTicks(ticks);
                ent.setLastDamage(lastDamage);
            }
        };
        r.runTaskLater(plugin, 1);
    }
    
    @EventHandler
	public void onJoin(PlayerJoinEvent event) {
        lastJoined.put(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis());
    }
    
    //[Animation Tracker]
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void CPSCheck(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        UUID uid = p.getUniqueId();
        AnimationMemory.put(uid, System.currentTimeMillis());
        //foodCooldowns(p);
    }
    
    @EventHandler
    public void webCooldown(BlockPlaceEvent event){
        if (event.getBlock().getType() == Material.WEB){
            UUID uid = event.getPlayer().getUniqueId();
            if(webCooldown.containsKey(uid) && System.currentTimeMillis() - webCooldown.get(uid) < 5000){
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Cooldown not complete for web placement.");
            } else {
                ItemStack[] inv = event.getPlayer().getInventory().getContents();
                //boolean isTagged = false;
                //if(plugin.getCPT().getTagManager().isTagged(event.getPlayer().getUniqueId())){
                //    isTagged = true;
                //}
                for (ItemStack is : inv){
                    if (is != null && (is.getType().name().contains("HELMET")
                            || is.getType().name().contains("LEGGINGS")
                            || is.getType().name().contains("BOOTS")
                            || is.getType().name().contains("CHESTPLATE"))){
                        webCooldown.put(uid, System.currentTimeMillis());
                        event.getPlayer().setCooldown(Material.WEB, 100);
                        return;
                    }
                }
            }
        }
    }

	@EventHandler
	public void livingEntityDamage(EntityDamageEvent event) {
        //LOG.info("[CivPVP debug] received a damage event.");
        if(event.getCause() == DamageCause.THORNS){
            return;
        }
        if(event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK){
            event.setCancelled(true);
            return;
        }
		if (event.getEntity() instanceof LivingEntity) { // not just player! zombies have armor, wolves get stabbed by
															// swords, etc
             
            //LOG.info("-----------------------------------------");
            //LOG.info("original health: " + ((LivingEntity)event.getEntity()).getHealth());
                        
            //remove weird buggy ghost cows and shit on hitting them
            if (Double.isNaN(((LivingEntity)event.getEntity()).getHealth()) && !(event.getEntity() instanceof Player)){
                Location nanent = event.getEntity().getLocation();
                String damagerUUID = "none";
                if (event instanceof EntityDamageByEntityEvent){
                    damagerUUID = ((EntityDamageByEntityEvent) event).getDamager().getUniqueId().toString();
                }
                LOG.info("CRG(P): NaN Health? : " + ((LivingEntity)event.getEntity()).getHealth() + " at " + nanent.getX() + ", " + nanent.getY() + ", " + nanent.getZ() + " damager: " + damagerUUID);
                event.getEntity().remove();
                return;
            }
                                                            
            DamageCause cause = event.getCause(); //ENTITY_ATTACK and ENTITY_SWEEP_ATTACK are melee, PROJECTILE arrow.
            boolean projectile = false;
            if (cause == DamageCause.PROJECTILE){projectile = true;}
            ArmorProfile ar = calcArmorInfo((LivingEntity)event.getEntity(),projectile); //the projectile boolean here is only used for mob/player on mob/player damage which is either melee or not, won't affect falls and fire and stuff
            
            //anti portal transit lag related deaths
            if(cause == DamageCause.VOID || cause == DamageCause.SUFFOCATION || cause == DamageCause.DROWNING){
                if (event.getEntity() instanceof Player){
                    Player player = ((Player)event.getEntity());
                    if (lastJoined.containsKey(player.getUniqueId().toString()) && System.currentTimeMillis() - lastJoined.get(player.getUniqueId().toString()) < 20000 && event.getEntity().getWorld().getName().equals("aqua")){
                        //if it's a player, and they've been logged in < 30 seconds, and they're in the AN, cancel this damage and return.
                        Location pl = event.getEntity().getLocation();
                        World w = event.getEntity().getWorld();
                        for (int x = pl.getBlockX()-2; x < pl.getBlockX()+2; x++){
                            for (int z = pl.getBlockZ()-2; z < pl.getBlockZ()+2; z++){
                                if (w.getBlockAt(x,254,z).getType() != Material.BEDROCK){
                                    event.setCancelled(true);
                                    LOG.info("CRG: Drown/Suffocation/Void damage negated near portal after login for " + player.getDisplayName() + ".");
                                    return;
                                }
                            }
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (cause == DamageCause.FALL){
                if (event.getEntity() instanceof Player){
                    Player player = ((Player)event.getEntity());
                    if (lastJoined.containsKey(player.getUniqueId().toString()) && System.currentTimeMillis() - lastJoined.get(player.getUniqueId().toString()) < 20000 && !event.getEntity().getWorld().getName().contains("aqua") && !event.getEntity().getWorld().getName().contains("prison")){
                        //if it's a player, and they've been logged in < 30 seconds, and they're in the overworld, check for above a portal...
                        Location pl = player.getLocation();
                        World w = player.getWorld();
                        for (int x = pl.getBlockX()-2; x < pl.getBlockX()+2; x++){
                            for (int z = pl.getBlockZ()-2; z < pl.getBlockZ()+2; z++){
                                if (w.getBlockAt(x,0,z).getType() != Material.BEDROCK){
                                    event.setCancelled(true);
                                    LOG.info("CRG: Fall damage negated near portal after login for " + player.getDisplayName() + ".");
                                    return;
                                }
                            }
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            
            //This section calculates not only the % armor absorption we want, but the re-scaled actual raw amount.
            //This second part is in case it is NOT player on player damage, such as falling or whatever
            //If it is player on player, the second part will end up being ignored and only the target % used, since we have custom damage there (only)
            double armorAbsorbPercentTarget = ar.armorAbsorbTarget*-1;
            armorAbsorbPercentTarget = armorAbsorbPercentTarget - ((int)armorAbsorbPercentTarget);
            double armorAbsorb = event.getOriginalDamage(DamageModifier.ARMOR); //negative number
            double armorAbsorbPercent = armorAbsorb / event.getDamage();
            double newArmorAbsorption = armorAbsorb;
            if (armorAbsorbPercent != 0){
                newArmorAbsorption = (armorAbsorb*(armorAbsorbPercentTarget/armorAbsorbPercent));
                //LOG.info("new armor absorb " + newArmorAbsorption + " old armor % " + armorAbsorbPercent + " new armor % " + armorAbsorbPercentTarget);
                if(!Double.isNaN(newArmorAbsorption)){event.setDamage(DamageModifier.ARMOR,newArmorAbsorption);} //else {event.setDamage(event.getDamage());}
            }
            
            //Same as above but for the enchantment portion of the damage absorption
            //and all is only for pvp (or mobs etc):
            double avgEnchantLevel = ar.armorEnchant / 4.0;
            double enchantAbsorption = event.getOriginalDamage(DamageModifier.MAGIC); //used further down for default case
            double enchantAbsorbPercent = enchantAbsorption / event.getDamage();
            double newEnchantAbsorption = enchantAbsorption;
            double enchantAbsorbTargetPercent = (1-(1/Math.pow(1.17,avgEnchantLevel)))*-1;//amount that gets through is 1 / 1.17 for level 1, / 1.17 for level 2... protection is 1- that.
            if (cause.equals(DamageCause.PROJECTILE) || cause.equals(DamageCause.ENTITY_ATTACK) || cause.equals(DamageCause.ENTITY_SWEEP_ATTACK)){
                //LOG.info("apply levels based enchant absorption, levels: " + avgEnchantLevel);
                newEnchantAbsorption = (enchantAbsorption*(enchantAbsorbTargetPercent/enchantAbsorbPercent));
                //LOG.info("newEAbs" +  newEnchantAbsorption + " oldEA percent " + enchantAbsorbPercent + " new EA percent " + enchantAbsorbTargetPercent);
                if(!Double.isNaN(newEnchantAbsorption)){event.setDamage(DamageModifier.MAGIC,newEnchantAbsorption);} //else {event.setDamage(event.getDamage());} //just all enchants two thirds effective for now.
            }
            
                        
            //default "weapon" damage 1.5 covers fists and stuff and mobs like spiders
            //checks for projectile or melee because I'm not sure if fire damage while in a fire is "entity on entity" or not, this just clearly makes it mobs and players only.
            double weaponTargetDamage = 0.75;
            if (event instanceof EntityDamageByEntityEvent && (cause == DamageCause.PROJECTILE || cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK)){
                
                //implement my own immunity period, only for enforcing times between other entity damages, intentionally not considering falls etc.
                //mobs get immunity too so it's up near the top before "instanceof player"
                UUID uid = event.getEntity().getUniqueId();
                if (lastDamagedByEntity.containsKey(uid) && System.currentTimeMillis() - lastDamagedByEntity.get(uid) < plugin.noTicks*50){ //50 ms per tick
                    event.setCancelled(true);
                    return;
                }
                
                //cps ANIMATION EVENT VERSION, up here at top because it applies to mobs too
                Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
                UUID attackerUUID = attacker.getUniqueId();
                if(!AnimationMemory.containsKey(attackerUUID)){
                    AnimationMemory.put(attackerUUID, System.currentTimeMillis()-1000); //allow first hit to pass if hasn't swung yet since logging on
                }
                long AnimationGap = (System.currentTimeMillis()-AnimationMemory.get(attackerUUID));
                double cpsMultiplier;
                if (AnimationGap > 238){ //6.8 cps or lower, 3 ticks at 20 tps, 2 ticks at 13 tps or lower
                    cpsMultiplier = 1.0; //100% 
                } else if (AnimationGap > 147) {
                    cpsMultiplier = 0.9; //80%
                } else if (AnimationGap > 80) {
                    cpsMultiplier = 0.7; //60%
                } else {
                    cpsMultiplier = 0; //0% I guess not necessary whatever
                    event.setCancelled(true);
                    if (attacker instanceof Player){
                        attacker.sendMessage(ChatColor.RED + "Attacking too quickly.");
                    }
                    return;
                }
                
                //protect squid from deathrays so that guardians don't take over the whole mob cap
                if(event.getEntity().getType().equals(EntityType.SQUID) && ((EntityDamageByEntityEvent)event).getDamager().getType().equals(EntityType.GUARDIAN)){
                    event.setCancelled(true);
                    return;
                }
                
                if(event.getEntity().getType().equals(EntityType.ENDERMITE)){
                    if (event.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK)){
                        long now = System.currentTimeMillis();
                        if (now - lastMiteSweep < 50){
                            //event.setDamage(1);
                            event.setCancelled(true);
                        } else {
                            lastMiteSweep = now;
                        }
                    } else if (event.getCause().equals(DamageCause.LAVA) || event.getCause().equals(DamageCause.FALL) 
                            || event.getCause().equals(DamageCause.FALLING_BLOCK) || event.getCause().equals(DamageCause.SUFFOCATION)){
                        event.setCancelled(true);
                    }
                    return;
                }
                
                //choose weapon properly for bows or other weapons (bows require tracing back the ownership of the arrow...)
                ItemStack weapon = null;
                if (((EntityDamageByEntityEvent)event).getDamager() instanceof Arrow && ((Arrow)(((EntityDamageByEntityEvent)event).getDamager())).getShooter() instanceof Player){
                    weapon = ((Player)(((Arrow)(((EntityDamageByEntityEvent)event).getDamager())).getShooter())).getInventory().getItemInMainHand();
                }
                else if(((EntityDamageByEntityEvent)event).getDamager() instanceof Player){
                    weapon = ((Player)((EntityDamageByEntityEvent)event).getDamager()).getInventory().getItemInMainHand();
                }
                
                //if it's a real weapon, substitute in special civrealms damage amounts
                if (weapon != null){
                    if (weapon.getType() == Material.WOOD_SWORD && weapon.hasItemMeta() && weapon.getItemMeta().hasEnchant(Enchantment.KNOCKBACK)){
                        event.setCancelled(true);
                        if((attacker instanceof Player)){
                            ((Player)attacker).sendMessage(ChatColor.RED + "KB test swords are disabled until further notice / until new testing is needed.");
                        }
                    }
                        if (weapon.getType().equals(Material.BOW)){
                           //LOG.info("arrow vel: " + ((Arrow)((EntityDamageByEntityEvent)event).getDamager()).getVelocity());
                           Vector v = ((Arrow)((EntityDamageByEntityEvent)event).getDamager()).getVelocity();
                           weaponTargetDamage = (Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY()+v.getZ()*v.getZ())/3.0) * 6.0*damageModifier; //7.5 typical full draw across a short field. 100 blocks above full draw straight down would be 9.3, limp wristed point blank 1.2 damage.
                        } else if (weapon.getType().equals(Material.WOOD_SWORD)){
                            weaponTargetDamage = 0.1*damageModifier; //2.07
                        } else if (weapon.getType().equals(Material.STONE_SWORD) || weapon.getType().equals(Material.STONE_AXE)){
                            weaponTargetDamage = 1.2*damageModifier; //2.4; //2.07
                        } else if (weapon.getType().equals(Material.GOLD_SWORD) || weapon.getType().equals(Material.GOLD_AXE)){
                            weaponTargetDamage = 2.65*damageModifier; //5.3; //3.94
                        } else if (weapon.getType().equals(Material.IRON_SWORD) || weapon.getType().equals(Material.IRON_AXE)){
                            if (weapon.hasItemMeta() && weapon.getItemMeta().hasLore() && weapon.getItemMeta().getLore().get(0).equals("Made of tempered steel")){
                                weaponTargetDamage = 4*damageModifier; //8; //5.44
                            } else {
                                weaponTargetDamage = 1.8*damageModifier; //3.6; //2.86
                            }
                        } else if (weapon.getType().equals(Material.DIAMOND_SWORD) || weapon.getType().equals(Material.DIAMOND_AXE)){
                            weaponTargetDamage = 6.6*damageModifier; //13.2; //7.5
                        }
                        
                        //boost damage for enchanted weapons
                        if (weapon.getEnchantments().containsKey(Enchantment.ARROW_DAMAGE)){ //power
                            weaponTargetDamage = weaponTargetDamage * Math.pow(1.17,weapon.getEnchantments().get(Enchantment.ARROW_DAMAGE)); //square root of 1.38, so two enchantment levels = one tier of base armor or weapon grade
                        } else if (weapon.getEnchantments().containsKey(Enchantment.DAMAGE_ALL)){ //sharpness
                            weaponTargetDamage = weaponTargetDamage * Math.pow(1.17,weapon.getEnchantments().get(Enchantment.DAMAGE_ALL));
                        }
                        
                        //LOG.info("pre cps or crit modifier, target damage: " + weaponTargetDamage);
                        
                        //double damage versus mobs, prior to calculating absolute armor amounts just on the chance the mob has armor or pvp relevant enchants
                        if(!(event.getEntity() instanceof Player)){
                            weaponTargetDamage = weaponTargetDamage * 2.0;
                        }
                        
                        //now calculate the final 3 values for player on player, damage, armor absorb abs amount and enchant absorb abs amount:
                        weaponTargetDamage = weaponTargetDamage * cpsMultiplier;
                        double armorTargetDamageRemoved = armorAbsorbPercentTarget * weaponTargetDamage; //arbitrarily choose to apply armor part first
                        double enchantTargetDamageRemoved = (weaponTargetDamage + armorTargetDamageRemoved) * enchantAbsorbTargetPercent; //enchant mops up a % OF THE REMAINDER only.
                        
                        //then apply them:
                        if(!Double.isNaN(weaponTargetDamage)){event.setDamage(weaponTargetDamage);}
                        if(!Double.isNaN(armorTargetDamageRemoved)){event.setDamage(DamageModifier.ARMOR,(armorTargetDamageRemoved));}
                        if(!Double.isNaN(enchantTargetDamageRemoved)){event.setDamage(DamageModifier.MAGIC,enchantTargetDamageRemoved);}
                        
                        if((event.getEntity() instanceof Player)){
                            LOG.info("Name of person hit: " + ((Player)event.getEntity()).getDisplayName() + " ill fitting penalty: " + ar.illFittingPenalty);
                            LOG.info("Health before: " + ((Player)event.getEntity()).getHealth() + " cps modifier: " + cpsMultiplier + " Armor = " + armorTargetDamageRemoved + " Enchant = " + enchantTargetDamageRemoved + " Dmg = " + weaponTargetDamage + " Net = " + (weaponTargetDamage + armorTargetDamageRemoved + enchantTargetDamageRemoved));
                        }
                        //LOG.info("origDamage" + event.getDamage());
                        //LOG.info("cps modifier: " + cpsMultiplier + " animation gap: " + AnimationGap);
                        //LOG.info("post cps modifier, damage: " + event.getDamage());
                        //LOG.info("FINAL ARMOR ABSORB = " + armorTargetDamageRemoved + " FINAL ENCHANT ABSORB = " + enchantTargetDamageRemoved + " WEAPON TARGET DMG = " + weaponTargetDamage);
                        //LOG.info("Net pvp damage result" + (weaponTargetDamage + armorTargetDamageRemoved + enchantTargetDamageRemoved)); 
                        
                    }
                
                    //KNOCKBACK MODIFICATION: regular hits seem to add about 0.5 to the Y so just subtract that right back again. Maybe also a tiny bit back but that's fine.
                    /*Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
                    Entity damaged = event.getEntity();
                    double knockBackVertTemp = 1.0;
                    double knockBackAwayTemp = 1.0;
                    double awayNormalTemp = this.plugin.awayFromYouDampener;

                    if (damager instanceof Player){
                        
                        weapon = ((Player)damager).getInventory().getItemInMainHand();
                        if (weapon != null && weapon.getType() == Material.WOOD_SWORD){ //for testing at first.
                            if (weapon != null && weapon.getEnchantments() != null && weapon.getEnchantments().containsKey(Enchantment.KNOCKBACK)){
                                //if a knockback weapon, set both multipliers to knockback ones
                                knockBackAwayTemp = this.plugin.knockBackEnchantMultiplierAway;
                                knockBackVertTemp = this.plugin.knockBackEnchantMultiplierVertical;
                                //be able to disable wooden knockbacks after testing period
                                if (weapon.getType() == Material.WOOD_SWORD && !this.plugin.woodTestingOn){
                                    event.setCancelled(true);
                                    damager.sendMessage(ChatColor.RED + "Wooden Knockback testing is disabled currently.");
                                    return;
                                }
                            }
                            //ok so knockback knocks you directly away some amount AND for some reason it also bumps you straight up no matter what angle you were hit from
                            //so we want to dampen the vertical, and separately also dampen the direction away from the hit.
                            Vector originalVector = damaged.getVelocity();
                            Location damagerLoc = damager.getLocation();
                            Location damagedLoc = damaged.getLocation();
                            Vector relVector = new Vector(damagerLoc.getX()-damagedLoc.getX(),damagerLoc.getY()-damagedLoc.getY(),damagerLoc.getZ()-damagedLoc.getZ()); //relative between them, for the "away from hit" part
                            relVector = relVector.normalize(); //make it magnitude 1 no matter how far apart they are.
                            if (damaged.isOnGround()){
                                awayNormalTemp = awayNormalTemp * 2; //more dampening in water/air
                            }
                            relVector = relVector.multiply(awayNormalTemp*knockBackAwayTemp); //make it smaller, positive already means toward the hitter due to the order of subtraction above.
                            if (damaged.isOnGround()){
                                relVector = relVector.setY(relVector.getY() - this.plugin.hitVertical*knockBackVertTemp);
                            }
                            if (event.getDamage() > 0 && event.isCancelled() == false){
                                Vector newVector = originalVector.add(relVector);
                                damaged.setVelocity(newVector);
                            }
                        }
                    }*/
                    
                    //nodamageticks apply whether or not it's a human hitting, whether or not it has a weapon, etc.
                    //disabled for now because I made my own immunity period instead further up.
                    /*if (event.getDamage() > 0 && event.isCancelled() == false){
                        if(event.getEntity() instanceof LivingEntity){
                            setNoTicksAndLastDamage(((LivingEntity)event.getEntity()),this.plugin.noTicks,event.getDamage());
                            lastDamagedByEntity.put(event.getEntity().getUniqueId(),System.currentTimeMillis());
                            LOG.info("[CivPVP Debug] hit landed at " + System.currentTimeMillis() + " on " + event.getEntity().getUniqueId());
                        }
                    }*/
                }
            else {
                //LOG.info("FINAL ARMOR ABSORB = " + armorAbsorb*(armorAbsorbPercentTarget/armorAbsorbPercent) + " FINAL ENCHANT ABSORB = " + newEnchantAbsorption*0.33);
            }
            //LOG.info("FINAL INPUT DAMAGE = " + event.getDamage() + " FINAL DAMAGE = " + event.getFinalDamage());
                  
            //SPECIAL DAMAGE MODIFYING AREAS, so far only falling is modified from vanilla.
			if (cause.equals(DamageCause.FALL)) {
                if (event.getEntity() instanceof Player){
                    if (((Player)event.getEntity()).getLocation().getBlock().getType().equals(Material.BIRCH_FENCE) 
                            || ((Player)event.getEntity()).getLocation().getBlock().getRelative(0,-1,0).getType().equals(Material.BIRCH_FENCE)){
                        event.setCancelled(true); //negate fall damage at the top or bottom of scaffolding
                        return;
                    }
                }
			}
            
            //decipe whether to apply health pot auto splash
            if (event.getEntity() instanceof Player) {
                Player p = (Player)event.getEntity();
                int potCount = -1;
                boolean potUsed = false;
                if (p.getHealth() < 6){
                    for (ItemStack is : p.getInventory().getContents()){
                        if (is != null && is.getType().equals(Material.SPLASH_POTION)){
                            PotionMeta pm = (PotionMeta)is.getItemMeta();
                            double currHealthPercent = (p.getHealth()/p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                            if (pm.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL) && currHealthPercent < 0.3 && !potUsed){ //checks health again so it won't use them all at once. First check is to avoid searching an entinre inventory every damage.
                                is.setAmount(0);
                                p.setHealth(Math.min(currHealthPercent+0.3,1.0)*p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
                                p.sendMessage(ChatColor.GREEN + "Healing Potion Automatically Used");
                                p.getWorld().playEffect(p.getLocation(),Effect.POTION_BREAK, 0xFFFF0000);
                                //p.getWorld().spawnParticle(Particle.SPELL_INSTANT, p.getLocation(),50);
                                potUsed = true; //I want to keep looping to count all the pots even if one already used, but not use 2+ of them
                                potCount++;
                            }
                        }
                    }
                }
                LOG.info("Health pots they had available (-1 is N/A): " + potCount);
            }
		}
	}
    
    //only used for pvp or mob fighting, don't worry about this returning zero for fire etc.
    public int getArmorEnchantLevel(ItemStack armor, boolean projectile){
        if (armor.getEnchantments().containsKey(Enchantment.PROTECTION_ENVIRONMENTAL)){
            return armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
        } else if (armor.getEnchantments().containsKey(Enchantment.PROTECTION_PROJECTILE)){
            if (projectile){
                return armor.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE)+1;
            } else {
                return 0;
            }
        } else {
            return 0;            
        }
    }
    
    //the physical armor part of this is for pvp and also other things. The enchant part is only for pvp
    public ArmorProfile calcArmorInfo(LivingEntity e, boolean projectile){ //boolean only relevant to pvp, either true or false for melee, otherwise either.
        ItemStack boots = e.getEquipment().getBoots();
        ItemStack legs = e.getEquipment().getLeggings();
        ItemStack chest = e.getEquipment().getChestplate();
        ItemStack helmet = e.getEquipment().getHelmet();
        
        double illFittingPenalty = 1; //1 is no penalty, because it's a multiplier
        double armorAbsorbTarget = 0;
        int armorEnchant = 0;
        int pieces = 0;
        
        if (e instanceof Horse){
            ItemStack barding = ((Horse)e).getInventory().getArmor();
            if (barding != null){
                if (barding.getType().equals(Material.GOLD_BARDING)){
                    armorAbsorbTarget += 0.875; //.27
                    pieces++;
                } else if (barding.getType().equals(Material.DIAMOND_BARDING)){
                    armorAbsorbTarget += 0.95; //.62
                    pieces++;
                } else if (barding.getType().equals(Material.IRON_BARDING)){
                    if (barding.hasItemMeta() && barding.getItemMeta().hasLore() && barding.getItemMeta().getLore().contains("Made of tempered steel")){
                        armorAbsorbTarget += 0.92; //0.72
                        pieces++;
                    } else {
                        armorAbsorbTarget += 0.80; //0.72
                        pieces++;
                    }
                }
            }
        } else {
        
            if(boots != null){
                if (boots.getType() == Material.LEATHER_BOOTS){
                    armorAbsorbTarget += 0.38; //.27
                    pieces++;
                } else if (boots.getType() == Material.GOLD_BOOTS){
                    armorAbsorbTarget += 0.75; //.62
                    pieces++;
                } else if (boots.getType() == Material.CHAINMAIL_BOOTS){
                    armorAbsorbTarget += 0.60; //0.47
                    pieces++;
                } else if (boots.getType() == Material.IRON_BOOTS){
                    armorAbsorbTarget += 0.84; //0.72
                    pieces++;
                } else if (boots.getType() == Material.DIAMOND_BOOTS){
                    armorAbsorbTarget += 0.90; //.8
                    pieces++;
                }
                armorEnchant += getArmorEnchantLevel(boots,projectile); 
                //LOG.info("enchant level of boots: " + getArmorEnchantLevel(boots,projectile));
                if (e instanceof Player){
                    if ((boots.getType() == (Material.LEATHER_BOOTS) && boots.getItemMeta().getLore().size() == 1) 
                            || (boots.getType() == (Material.LEATHER_BOOTS) && boots.getItemMeta().getLore().size() == 2 && boots.getItemMeta().getLore().contains("Branded Item")) 
                            || (boots.getItemMeta().getLore().contains("No Owner"))){
                        ItemMeta meta = boots.getItemMeta();
                        ArrayList<String> loreArray = (ArrayList<String>)meta.getLore();
                        if (boots.getItemMeta().getLore().contains("No Owner")){
                            loreArray.remove(1);
                        }
                        loreArray.add(((Player)e).getDisplayName());
                        meta.setLore(loreArray);
                        boots.setItemMeta(meta);
                        e.sendMessage(ChatColor.GREEN +"Your boots are worn in by you.");
                    }
                    else if (boots.getType() != Material.CHAINMAIL_BOOTS && !boots.getItemMeta().getLore().contains(((Player) e).getDisplayName())){
                        illFittingPenalty -= 0.125; //one eighth less protection each, so all 4 ill fitting is 50% effective
                    }
                }
            }

            if(legs != null){
                if (legs.getType() == Material.LEATHER_LEGGINGS){
                    armorAbsorbTarget += 0.38; //.27
                    pieces++;
                } else if (legs.getType() == Material.GOLD_LEGGINGS){
                    armorAbsorbTarget += 0.75; //.62
                    pieces++;
                } else if (legs.getType() == Material.CHAINMAIL_LEGGINGS){
                    armorAbsorbTarget += 0.60; //0.47
                    pieces++;
                } else if (legs.getType() == Material.IRON_LEGGINGS){
                    armorAbsorbTarget += 0.84; //0.72
                    pieces++;
                } else if (legs.getType() == Material.DIAMOND_LEGGINGS){
                    armorAbsorbTarget += 0.90; //.8
                    pieces++;
                }
                armorEnchant += getArmorEnchantLevel(legs,projectile);
                //LOG.info("enchant level of legs: " + getArmorEnchantLevel(legs,projectile));
                if (e instanceof Player){
                    if ((legs.getType() == (Material.LEATHER_LEGGINGS) && legs.getItemMeta().getLore().size() == 1) 
                            || (legs.getType() == (Material.LEATHER_LEGGINGS) && legs.getItemMeta().getLore().size() == 2 && legs.getItemMeta().getLore().contains("Branded Item")) 
                            || (legs.getItemMeta().getLore().contains("No Owner"))){
                        ItemMeta meta = legs.getItemMeta();
                        ArrayList<String> loreArray = (ArrayList<String>)meta.getLore();
                        if (legs.getItemMeta().getLore().contains("No Owner")){
                            loreArray.remove(1);
                        }
                        loreArray.add(((Player)e).getDisplayName());
                        meta.setLore(loreArray);
                        legs.setItemMeta(meta);
                        e.sendMessage(ChatColor.GREEN +"Your leggings are worn in by you.");
                    }
                    else if (legs.getType() != Material.CHAINMAIL_LEGGINGS && !legs.getItemMeta().getLore().contains(((Player) e).getDisplayName())){
                        illFittingPenalty -= 0.125;
                    }
                }
            }

            if(chest != null){
                if (chest.getType() == (Material.LEATHER_CHESTPLATE)){
                    armorAbsorbTarget += 0.38; //.27
                    pieces++;
                } else if (chest.getType() == (Material.GOLD_CHESTPLATE)){
                    armorAbsorbTarget += 0.75; //.62
                    pieces++;
                } else if (chest.getType() == (Material.CHAINMAIL_CHESTPLATE)){
                    armorAbsorbTarget += 0.60; //0.47
                    pieces++;
                } else if (chest.getType() == (Material.IRON_CHESTPLATE)){
                    armorAbsorbTarget += 0.84; //0.72
                    pieces++;
                } else if (chest.getType() == (Material.DIAMOND_CHESTPLATE)){
                    armorAbsorbTarget += 0.90; //.8
                    pieces++;
                }
                armorEnchant += getArmorEnchantLevel(chest,projectile);
                //LOG.info("enchant level of chest: " + getArmorEnchantLevel(chest,projectile));
                if (e instanceof Player){
                    if ((chest.getType() == (Material.LEATHER_CHESTPLATE) && chest.getItemMeta().getLore().size() == 1) 
                            || (chest.getType() == (Material.LEATHER_CHESTPLATE) && chest.getItemMeta().getLore().size() == 2 && chest.getItemMeta().getLore().contains("Branded Item")) 
                            || (chest.getItemMeta().getLore().contains("No Owner"))){
                        ItemMeta meta = chest.getItemMeta();
                        ArrayList<String> loreArray = (ArrayList<String>)meta.getLore();
                        if (chest.getItemMeta().getLore().contains("No Owner")){
                            loreArray.remove(1);
                        }
                        loreArray.add(((Player)e).getDisplayName());
                        meta.setLore(loreArray);
                        chest.setItemMeta(meta);
                        e.sendMessage(ChatColor.GREEN +"Your chestplate is worn in by you.");
                    }
                    else if (chest.getType() != Material.CHAINMAIL_CHESTPLATE && !chest.getItemMeta().getLore().contains(((Player) e).getDisplayName())){
                        illFittingPenalty -= 0.125;
                    }
                }
            }

            if(helmet != null){
                boolean invalidHelmet = false;
                if (helmet.getType() == (Material.LEATHER_HELMET)){
                    armorAbsorbTarget += 0.38; //.27
                    pieces++;
                } else if (helmet.getType() == (Material.GOLD_HELMET)){
                    armorAbsorbTarget += 0.75; //.62
                    pieces++;
                } else if (helmet.getType() == (Material.CHAINMAIL_HELMET)){
                    armorAbsorbTarget += 0.60; //0.47
                    pieces++;
                } else if (helmet.getType() == (Material.IRON_HELMET)){
                    armorAbsorbTarget += 0.84; //0.72
                    pieces++;
                } else if (helmet.getType() == (Material.DIAMOND_HELMET)){
                    armorAbsorbTarget += 0.90; //.8
                    pieces++;
                } else {
                    invalidHelmet = true;
                }
                if (!invalidHelmet){
                    armorEnchant += getArmorEnchantLevel(helmet,projectile);
                    //LOG.info("enchant level of helmet: " + getArmorEnchantLevel(helmet,projectile));
                    if (e instanceof Player){
                        if ((helmet.getType() == (Material.LEATHER_HELMET) && helmet.getItemMeta().getLore().size() == 1) 
                                || (helmet.getType() == (Material.LEATHER_HELMET) && helmet.getItemMeta().getLore().size() == 2 && helmet.getItemMeta().getLore().contains("Branded Item")) 
                                || (helmet.getItemMeta().getLore().contains("No Owner"))){
                            ItemMeta meta = helmet.getItemMeta();
                            ArrayList<String> loreArray = (ArrayList<String>)meta.getLore();
                            if (helmet.getItemMeta().getLore().contains("No Owner")){
                                loreArray.remove(1);
                            }
                            loreArray.add(((Player)e).getDisplayName());
                            meta.setLore(loreArray);
                            helmet.setItemMeta(meta);
                            e.sendMessage(ChatColor.GREEN +"Your helmet is worn in by you.");
                        }
                        else if (helmet.getType() != Material.CHAINMAIL_HELMET && !helmet.getItemMeta().getLore().contains(((Player) e).getDisplayName())){
                            illFittingPenalty -= 0.125;
                        }
                    }
                }
            }
        }
        
        if (illFittingPenalty < 1 && armorAbsorbTarget > 0 && e instanceof Player && Math.random() < 0.2){
            e.sendMessage(ChatColor.RED + "Your ill-fitting armor pinches, chafes, and isn't helping much.");
        }
        
        if (!(e instanceof Horse)){
            armorAbsorbTarget = ((armorAbsorbTarget) / 4)*illFittingPenalty; //average
        } //if it is a horse, there's no ill fitting and there's no 4 pieces of armor, so do nothing.
        
        return new ArmorProfile(illFittingPenalty,armorAbsorbTarget,armorEnchant);
        //return -1*armorAbsorbTarget + -1*armorEnchant; //final version is treated as "negative damage" for absorbption, enchant stored in non decimal portion temporarily.
    }
    
}
