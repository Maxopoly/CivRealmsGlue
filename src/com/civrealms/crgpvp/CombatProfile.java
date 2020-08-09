package com.civrealms.crgpvp;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

/**
 * @author Crimeo
 */
public class CombatProfile {
	private String UUID;
	private ItemStack[] armor;
	private boolean shield;
	private double blastProtection;
	private double fireProtection;
	private double coldProtection;
	private double physicalProtection; // EXCEPT fall damage. Includes weapons as well as falling blocks etc.
	private double fallProtection;
	private double weaponFire;
	private double weaponPhysical;
	private double thorns;
	private int weaponType;

	public CombatProfile(Player p, int weaponType) {
		setProfile(p, weaponType);
	}

	public void setProfile(Player p, int weaponType) {
		this.UUID = p.getUniqueId().toString();
		this.armor = p.getInventory().getArmorContents();
		this.shield = p.getInventory().getItemInOffHand().getType() == Material.SHIELD;
		this.weaponType = weaponType; // 1=main hand melee, 2=off hand melee, 3=bow, 4=thorns

		// and for weapons:
		this.weaponFire = 0; // fire damage
		this.weaponPhysical = 0;
		this.thorns = 0;
	}

	public String getUUID() {
		return UUID;
	}

	public void setUUID(String UUID) {
		this.UUID = UUID;
	}

	public ItemStack[] getArmor() {
		return armor;
	}

	public void setArmor(ItemStack[] armor) {
		this.armor = armor;
	}

	public boolean isShield() {
		return shield;
	}

	public void setShield(boolean shield) {
		this.shield = shield;
	}

	public double getBlastProtection() {
		return blastProtection;
	}

	public void setBlastProtection(double blastProtection) {
		this.blastProtection = blastProtection;
	}

	public double getFireProtection() {
		return fireProtection;
	}

	public void setFireProtection(double fireProtection) {
		this.fireProtection = fireProtection;
	}

	public double getColdProtection() {
		return coldProtection;
	}

	public void setColdProtection(double coldProtection) {
		this.coldProtection = coldProtection;
	}

	public double getPhysicalProtection() {
		return physicalProtection;
	}

	public void setPhysicalProtection(double physicalProtection) {
		this.physicalProtection = physicalProtection;
	}

	public double getFallProtection() {
		return fallProtection;
	}

	public void setFallProtection(double fallProtection) {
		this.fallProtection = fallProtection;
	}

	public double getWeaponFire() {
		return weaponFire;
	}

	public void setWeaponFire(double weaponFire) {
		this.weaponFire = weaponFire;
	}

	public double getWeaponPhysical() {
		return weaponPhysical;
	}

	public void setWeaponPhysical(double weaponPhysical) {
		this.weaponPhysical = weaponPhysical;
	}

	public double getThorns() {
		return thorns;
	}

	public void setThorns(double thorns) {
		this.thorns = thorns;
	}

}
