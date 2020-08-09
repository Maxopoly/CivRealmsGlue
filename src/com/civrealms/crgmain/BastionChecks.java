package com.civrealms.crgmain;

import isaac.bastion.BastionBlock;
import isaac.bastion.manager.BastionBlockManager;
import java.util.Set;
import org.bukkit.block.Block;

/**
 * @author Crimeo
 */
public class BastionChecks {
    
    public static Set<BastionBlock> getBastions(Block b){
        BastionBlockManager bbm = isaac.bastion.Bastion.getBastionManager();
        Set<BastionBlock> blocking = bbm.getBlockingBastions(b.getLocation());
        return blocking;
    }    
}
