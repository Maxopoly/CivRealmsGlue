package com.civrealms.crgcrops;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.Inventory;
import org.bukkit.block.Block;
import org.bukkit.Material;

/**
 * @author Crimeo
 */
public class ReplantTask extends BukkitRunnable {
    
    private final JavaPlugin plugin;
    Inventory inv = null;
    Block b = null;
    Material targetSeed = null;
    Material targetCrop = null;

    public ReplantTask(JavaPlugin plugin, Inventory inv, Block b, Material targetSeed, Material targetCrop) {
        this.plugin = plugin;
        this.inv = inv;
        this.b = b;
        this.targetSeed = targetSeed;
        this.targetCrop = targetCrop;
    }
    
    @Override
    public void run() {
        if (targetCrop == Material.COCOA){ 
            return;
        }
        for (int i = 0; i < 9; i++){
            //if (inv.getItem(i) != null && ((targetSeed != Material.INK_SACK && inv.getItem(i).getType() == targetSeed) || (targetSeed == Material.INK_SACK && inv.getItem(i).getType() == targetSeed && inv.getItem(i).getDurability()==3))){
            if (inv.getItem(i) != null && inv.getItem(i).getType() == targetSeed){
                if(b.getType() == Material.AIR){
                    
                    //OKAY FUCK IT, cocoa is not supported. This was attempt number like 35, what the hell is wrong with cocoa?
                    //Will leave here for any masochistic posterity
                    /*    b.setType(targetCrop);
                        BlockState state = b.getState();
                        if(b.getRelative(-1,0,0).getType() == Material.WOOD && ((Wood)b.getState().getData()).getSpecies() == TreeSpecies.JUNGLE){
                            CocoaPlant coco = new CocoaPlant(CocoaPlant.CocoaPlantSize.SMALL, BlockFace.WEST);
                            state.setData(coco);
                            state.update();
                        } else if (b.getRelative(1,0,0).getType() == Material.WOOD && ((Wood)b.getState().getData()).getSpecies() == TreeSpecies.JUNGLE){
                            CocoaPlant coco = new CocoaPlant(CocoaPlant.CocoaPlantSize.SMALL, BlockFace.EAST);
                            state.setData(coco);
                            state.update();
                        } else if (b.getRelative(0,0,-1).getType() == Material.WOOD && ((Wood)b.getState().getData()).getSpecies() == TreeSpecies.JUNGLE){
                            CocoaPlant coco = new CocoaPlant(CocoaPlant.CocoaPlantSize.SMALL, BlockFace.NORTH);
                            state.setData(coco);
                            state.update();
                        } else if (b.getRelative(0,0,1).getType() == Material.WOOD && ((Wood)b.getState().getData()).getSpecies() == TreeSpecies.JUNGLE){
                            CocoaPlant coco = new CocoaPlant(CocoaPlant.CocoaPlantSize.SMALL, BlockFace.SOUTH);
                            state.setData(coco);
                            state.update();
                        }
                    } else {
                    */
                        b.setType(targetCrop);
                        com.untamedears.realisticbiomes.RealisticBiomes.plugin.growAndPersistBlock(b, false, null, null, null);
                    //}
                    inv.getItem(i).setAmount(inv.getItem(i).getAmount()-1);
                }
                return;
            }
        }
    }
}
