package com.civrealms.crgpvp;

/**
 * @author Crimeo
 */
public class ArmorProfile {
    public double illFittingPenalty;
    public double armorAbsorbTarget;
    public double armorEnchant;
    
    public ArmorProfile(double illFittingPenalty, double armorAbsorbTarget, double armorEnchant){
        this.armorEnchant = armorEnchant;
        this.armorAbsorbTarget = armorAbsorbTarget;
        this.illFittingPenalty = illFittingPenalty;
    }
}
