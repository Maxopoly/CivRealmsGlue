package com.civrealms.crgmain;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * @author Crimeo
 */
public class CitadelChecks {
    public static boolean isReinforced(Block b, Player p){
        boolean isReinforced = false;
        if (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().isReinforced(b)
                && !((vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b) instanceof vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)
                    && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)(vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b))).canBypass(p)
                    && vg.civcraft.mc.citadel.PlayerState.get(p).isBypassMode())
                && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b)).getHealth() > 0.05) {
            //So logic: if there's a reinforcement and NOT all of the following are true:(it's a player reinforcement; the player ha sbypass perms on it; the player is set to bypass mode) AND health is > 5%
			isReinforced = true;
		}
        return isReinforced;
    }
    
    public static boolean hasReinforcementAtAll(Block b){
        boolean isReinforced = false;
        if (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().isReinforced(b)
                && (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b) instanceof vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)) {
            isReinforced = true;
		}
        return isReinforced;
    }
    
    public static boolean playerHasCropAccess(Block b, Player p){
        if (vg.civcraft.mc.citadel.Citadel.getReinforcementManager().isReinforced(b.getRelative(0,-1,0))
                && !((vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b.getRelative(0,-1,0)) instanceof vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)
                    && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)(vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b.getRelative(0,-1,0)))).canAccessCrops(p))
                && ((vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement)vg.civcraft.mc.citadel.Citadel.getReinforcementManager().getReinforcement(b.getRelative(0,-1,0))).getHealth() > 0.05) {
            return false;
        }
        return true;
    }
    
}
