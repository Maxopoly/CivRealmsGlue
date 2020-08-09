package com.civrealms.crgcrops;

import com.civrealms.crgmain.CivRealmsGlue;
import com.civrealms.crgpve.SekkritClass;
import java.util.ArrayList;
import org.bukkit.CropState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Crops;

/**
 * @author Crimeo
 */
public class BlockBreakListenerCrops  implements Listener {
    private CivRealmsGlue plugin; 
	
    public BlockBreakListenerCrops(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockBreakCrops(BlockBreakEvent event) { 
        boolean isReinforced = com.civrealms.crgmain.CitadelChecks.isReinforced(event.getBlock(),event.getPlayer());
        boolean playerHasCropAccess = com.civrealms.crgmain.CitadelChecks.playerHasCropAccess(event.getBlock(),event.getPlayer());
        //plugin.getLogger().info("ir: " + isReinforced + " ibur: " + isBlockUnderReinforced);
        Block block = event.getBlock();
        
        if(SekkritClass.checkLaurel(block)){ //sekkrit requirements
            if (isReinforced){return;}
            ItemStack items = new ItemStack(Material.LOG_2, 1, (short)1);
            ItemMeta meta = items.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            meta.setDisplayName("Laurel");
            lore.add("Laurel Wood");
            meta.setLore(lore);
            items.setItemMeta(meta);
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), items);
            return;
        } else if (SekkritClass.checkTea(block)){ //sekkrit requirements
            if (isReinforced){return;}
            ItemStack items = new ItemStack(Material.WATER_LILY, 1, (short)1);
            ItemMeta meta = items.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            meta.setDisplayName("Tea Leaves");
            lore.add("For putting in drinks, not harbors.");
            meta.setLore(lore);
            items.setItemMeta(meta);
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), items);
            return;
        } else if (block.getType().equals(Material.CROPS) && playerHasCropAccess){
            com.untamedears.realisticbiomes.RealisticBiomes.plugin.getPlantManager().removePlant(event.getBlock());
            if(!plugin.getNoAutoReplant().contains(event.getPlayer().getUniqueId())) {
                new ReplantTask(this.plugin, event.getPlayer().getInventory(), event.getBlock(), Material.SEEDS,
					Material.CROPS).runTaskLater(this.plugin, 6);
            }
		} else if (block.getType().equals(Material.CARROT) && playerHasCropAccess){
            com.untamedears.realisticbiomes.RealisticBiomes.plugin.getPlantManager().removePlant(event.getBlock());
            if(!plugin.getNoAutoReplant().contains(event.getPlayer().getUniqueId())) {
                new ReplantTask(this.plugin, event.getPlayer().getInventory(), event.getBlock(), Material.CARROT_ITEM,
					Material.CARROT).runTaskLater(this.plugin, 6);
            }
		} else if (block.getType().equals(Material.POTATO) && playerHasCropAccess){
            com.untamedears.realisticbiomes.RealisticBiomes.plugin.getPlantManager().removePlant(event.getBlock());
            if(!plugin.getNoAutoReplant().contains(event.getPlayer().getUniqueId())) {
                new ReplantTask(this.plugin, event.getPlayer().getInventory(), event.getBlock(), Material.POTATO_ITEM,
					Material.POTATO).runTaskLater(this.plugin, 6);
            }
		} else if (block.getType().equals(Material.BEETROOT_BLOCK) && playerHasCropAccess){
            com.untamedears.realisticbiomes.RealisticBiomes.plugin.getPlantManager().removePlant(event.getBlock());
            if(!plugin.getNoAutoReplant().contains(event.getPlayer().getUniqueId())) {
                new ReplantTask(this.plugin, event.getPlayer().getInventory(), event.getBlock(), Material.BEETROOT_SEEDS,
					Material.BEETROOT_BLOCK).runTaskLater(this.plugin, 6);
            }
		} else if (block.getType().equals(Material.NETHER_WARTS) && playerHasCropAccess){
            com.untamedears.realisticbiomes.RealisticBiomes.plugin.getPlantManager().removePlant(event.getBlock());
            if(!plugin.getNoAutoReplant().contains(event.getPlayer().getUniqueId())) {
                new ReplantTask(this.plugin, event.getPlayer().getInventory(), event.getBlock(), Material.NETHER_STALK,
					Material.NETHER_WARTS).runTaskLater(this.plugin, 6);
            }
		} else if ((block.getType().equals(Material.CROPS) || event.getBlock().getType().equals(Material.CARROT)
				|| event.getBlock().getType().equals(Material.POTATO)
				|| event.getBlock().getType().equals(Material.BEETROOT_BLOCK))) {
            if (!com.civrealms.crgmain.CitadelChecks.playerHasCropAccess(event.getBlock(), event.getPlayer())){
                vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement pr = ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(event.getBlock().getRelative(0, -1, 0)));
                if (pr.canAccessCrops(event.getPlayer())){
                    return;
                } else{
                    if(Math.random() < 0.25){ //reinforcements don't have an erode() or whatever method...?? Would prefer to use that so this multiplier was handled properly via config etc. but it's just not there
                        pr.setDurability(pr.getDurability()-1);
                    }
                    event.setCancelled(true);
                    return;
                }
            }
            else if(playerHasCropAccess){
                if (((Crops) block.getState().getData()).getState() != CropState.RIPE) {
                    ItemStack is;
                    if (event.getBlock().getType().equals(Material.CROPS)) {
                        is = new ItemStack(Material.SEEDS, 1);
                    } else if (event.getBlock().getType().equals(Material.CARROT)) {
                        is = new ItemStack(Material.CARROT_ITEM, 1);
                    } else if (event.getBlock().getType().equals(Material.POTATO)) {
                        is = new ItemStack(Material.POTATO_ITEM, 1);
                    } else {
                        is = new ItemStack(Material.BEETROOT_SEEDS, 1);
                    }
                    ItemMeta meta = is.getItemMeta();
                    ArrayList<String> lore = new ArrayList<String>();
                    lore.add("Unripe");
                    meta.setLore(lore);
                    if (!block.getType().equals(Material.CROPS)
                            && !event.getBlock().getType().equals(Material.BEETROOT_BLOCK)) {
                        is.setItemMeta(meta);
                    }
                    block.setType(Material.AIR);
                    block.getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                    event.setCancelled(true);
                }
            }
		} else if (event.getBlock().getType().equals(Material.NETHER_WARTS)) {
            if (!com.civrealms.crgmain.CitadelChecks.playerHasCropAccess(event.getBlock(), event.getPlayer())){
                vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement pr = ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(event.getBlock().getRelative(0, -1, 0)));
                if (pr.canAccessCrops(event.getPlayer())){
                } else{
                    if(Math.random() < 0.25){ //reinforcements don't have an erode() or whatever method...?? Would prefer to use that so this multiplier was handled properly via config etc. but it's just not there
                        pr.setDurability(pr.getDurability()-1);
                    }
                    event.setCancelled(true);
                }
            }
            else if(playerHasCropAccess){
                if (event.getBlock().getData() != 3) {
                    ItemStack is = new ItemStack(Material.NETHER_STALK, 1);
                    ItemMeta meta = is.getItemMeta();
                    ArrayList<String> lore = new ArrayList<String>(); // set lore to "Hand-Picked" only for
                                                                        // fruits/vegetables themsleves, not dedicated
                                                                        // seeds.

                    lore.add("Unripe");
                    meta.setLore(lore);
                    is.setItemMeta(meta);
                    event.getBlock().setType(Material.AIR);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                    event.setCancelled(true);
                }
            }
		} else if (event.getBlock().getType().equals(Material.COCOA)) {
            //fuck you cocoa. you just can't be reinforced, live with it.
			if ((event.getBlock().getData() < 8 || event.getBlock().getData() > 11)) {
                ItemStack is = new ItemStack(Material.INK_SACK, 1, (short) 3);
				ItemMeta meta = is.getItemMeta();
				ArrayList<String> lore = new ArrayList<String>();
                if (SekkritClass.checkCoffee(block)){
                    meta.setDisplayName("Coffee Beans");
                    lore.add("Bitter, Delicious Coffee");
                }
				lore.add("Unripe");
				meta.setLore(lore);
				is.setItemMeta(meta);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				event.setCancelled(true);
			}
        //meh people already figured out vines
		} else if (event.getBlock().getType() == Material.VINE){
            if (!isReinforced && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.SHEARS){
                int skyLight = event.getBlock().getLightFromSky();
                int howMuchStuffBelow = 0;
                for(int offset = 1; offset > -15; offset--){
                    if(event.getBlock().getY()-offset > 0){
                        if(event.getBlock().getRelative(0,offset,0).getType().isSolid()){
                            howMuchStuffBelow++;
                        }
                    }
                }
                if(skyLight > 9 && howMuchStuffBelow > 7){
                    if (Math.random() > (1.0-((skyLight-9)/6.0))+(1.0-((howMuchStuffBelow-7)/7.0))) {
                        ItemStack is;
                        ItemMeta meta;
                        if (event.getBlock().getX()<2000){
                            event.setDropItems(false);
                            is = new ItemStack(Material.CHORUS_FLOWER, 1);
                            meta = is.getItemMeta();
                            meta.setDisplayName("Hops");
                        } else {
                            event.setDropItems(false);
                            is = new ItemStack(Material.CHORUS_FRUIT_POPPED, 1);
                            meta = is.getItemMeta();
                            meta.setDisplayName("Grapes");
                        }
                        is.setItemMeta(meta);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                        event.getBlock().setType(Material.AIR);
                    } else if (Math.random() > 0.7) {
                        event.setDropItems(false);
                    }
                }
            }
        } else if (event.getBlock().getType() == Material.CACTUS) { //cactus isn't supposed to be reinforceable anyway
			Double rnd = Math.random();
			if (rnd < plugin.chorusFruitChance) {
				ItemStack is = new ItemStack(Material.CHORUS_FRUIT, 1);
				event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
				rnd = Math.random();
				if (rnd < plugin.antiInfinityCactusChance) {
					event.getBlock().setType(Material.AIR);
					event.setCancelled(true); // no infinite cacti re-breaking! but also don't want to mess up RB too
				}
			}
		}
    }
    
    @EventHandler
	public void onHeadBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.SKULL){
            Block block = event.getBlock();
            if (block.getWorld().getName().contains("new_world")){
                ItemStack is = new ItemStack(Material.INK_SACK, 1, (short) 3);
                ItemMeta meta = is.getItemMeta();
                ArrayList<String> lore = new ArrayList<String>();
                meta.setDisplayName("Coffee Beans");
                lore.add("Bitter, Delicious Coffee");
                lore.add("Unripe");
                meta.setLore(lore);
                is.setItemMeta(meta);
                event.getBlock().setType(Material.AIR);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), is);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
            }
        }
    }
}
