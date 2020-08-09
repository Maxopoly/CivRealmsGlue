package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomMobDrops implements Listener  {
	
	Random rand = new Random();
	private CivRealmsGlue plugin;
	public static Logger LOG = Logger.getLogger("CivRealmsGlue");

	public CustomMobDrops(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
	public ItemStack animalSkin(int amt, int levelEnchant) {
		int bonus = 0;
        if (Math.random()<0.33*levelEnchant){
            bonus = 1;
        }
        ItemStack is = new ItemStack(Material.ROTTEN_FLESH, amt+bonus);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Animal Skin");
		String la[] = new String[] { "Add to Alum", "to make Leather" };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}

	public ItemStack kno3(int amt, int levelEnchant) {
		int bonus = 0;
        if (Math.random()<0.33*levelEnchant){
            bonus = 1;
        }
        ItemStack is = new ItemStack(Material.SUGAR, amt+bonus);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Potassium Nitrate");
		String la[] = new String[] { "Gunpowder Ingredient" };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}
    
    public ItemStack scorpionMeat(int amt, int levelEnchant) {
		int bonus = 0;
        if (Math.random()<0.33*levelEnchant){
            bonus = 1;
        }
        ItemStack is = new ItemStack(Material.INK_SACK, amt+bonus, (short)7);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Scorpion Meat");
		String la[] = new String[] { "Use it to make a tasty stew!" };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}

	public ItemStack pearl(int amt, int levelEnchant) {
		int bonus = 0;
        if (Math.random()<0.10*levelEnchant){
            bonus = 1;
        }
        ItemStack is = new ItemStack(Material.ENDER_PEARL, amt+bonus);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Exile Pearl");
		String la[] = new String[] { "Use to exile other players.","Do not move pearls between worlds." };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}

	public ItemStack tears(int amt, int levelEnchant) {
        int bonus = 0;
        if (Math.random()<0.10*levelEnchant){
            bonus = 1;
        }
		ItemStack is = new ItemStack(Material.WATER_BUCKET, amt+bonus);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("Doxxer Tears");
		String la[] = new String[] { ":(" };
		List<String> lorearray = Arrays.asList(la);
		meta.setLore(lorearray);
		is.setItemMeta(meta);
		return is;
	}
    
    public ItemStack bones(int amt, int levelEnchant) {
        int bonus = 0;
        if (Math.random()<0.10*levelEnchant){
            bonus = 1;
        }
		ItemStack is = new ItemStack(Material.BONE, amt+bonus);
		return is;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void newdrops(EntityDeathEvent event) {
        int levelEnchant = 0;
        if (event.getEntity() instanceof LivingEntity && event.getEntity().getKiller() != null){
            ItemStack weapon = event.getEntity().getKiller().getInventory().getItemInMainHand();
            if (weapon.containsEnchantment(Enchantment.LOOT_BONUS_MOBS)){
                levelEnchant = weapon.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
            }
        }
		LivingEntity entity = (LivingEntity) event.getEntity();
		if (entity instanceof Ageable) {
			if (!((Ageable) entity).isAdult()) {
				return;
			}
		}
		if (event.getEntity().getKiller() == null) {
			if(!(event.getEntity() instanceof Player)) {
				event.getDrops().clear();
			}
			return;
		}

		EntityType et = event.getEntityType();

		// some things drop bones and flesh when they die
		if (et == EntityType.WOLF || et == EntityType.SHEEP) {
			event.getDrops().add(bones(2,levelEnchant));

			int random = (int) (Math.random() * 2.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));

            if (et == EntityType.WOLF){
                Wolf w = (Wolf)event.getEntity();
                if (w.getCustomName() != null && w.getCustomName().contains("Big")) {
                    ItemStack is2 = new ItemStack(tears(1,0));
                    event.getDrops().add(is2);
                }
            }
		} else if (et == EntityType.ENDERMITE){
            ItemStack is = new ItemStack(Material.ENDER_STONE, 1);
			event.getDrops().add(is);
        } else if (et == EntityType.SILVERFISH){
            ItemStack is3 = new ItemStack(scorpionMeat(1,levelEnchant));
			event.getDrops().add(is3);
        } else if (et == EntityType.HORSE || et == EntityType.DONKEY) {

            int cancelCount = 0;
            for (ItemStack is:event.getDrops()){
                if (cancelCount < 3 && (is.getType().equals(Material.LEATHER) || is.getType().equals(Material.ROTTEN_FLESH))){
                    is.setAmount(Math.max(is.getAmount()-3,0));
                    cancelCount++;
                }
            }
			int random = (int) (Math.random() * 5); // random number between 0 and 4 inclusive
			event.getDrops().add(bones(random,levelEnchant));

			random = (int) (Math.random() * 3.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));
		} else if (et == EntityType.SQUID) {

			event.getDrops().clear();

			int random = (int) (Math.random() * 3.5);
			ItemStack is = new ItemStack(Material.INK_SACK, random);
			event.getDrops().add(is);

			if (Math.random() > 0.7) {
				event.getDrops().add(new ItemStack(Material.RAW_FISH, 1));
			}

			double randomdoub = Math.random();
			if (randomdoub > 0.85  ) {
				event.getDrops().add(pearl(1,levelEnchant));
			}

		} else if (et == EntityType.COW || et == EntityType.MUSHROOM_COW) {

			event.getDrops().clear();

			int random = (int) (Math.random() * 4);
			event.getDrops().add(bones(random,levelEnchant));

			random = (int) (Math.random() * 3.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));

			random = (int) (Math.random() * 4);
			ItemStack is = new ItemStack(Material.RAW_BEEF, random);
			event.getDrops().add(is);

		} else if (et == EntityType.POLAR_BEAR) {

			event.getDrops().clear();

			int random = (int) (Math.random() * 4);
			event.getDrops().add(bones(random,levelEnchant));

			random = (int) (Math.random() * 4.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));

		} else if (et == EntityType.LLAMA) {

            int cancelCount = 0;
            for (ItemStack is:event.getDrops()){
                if (cancelCount < 3 && (is.getType().equals(Material.LEATHER) || is.getType().equals(Material.ROTTEN_FLESH))){
                    is.setAmount(Math.max(is.getAmount()-3,0));
                    cancelCount++;
                }
            }
			//event.getDrops().clear(); //same as horses

			int random = (int) (Math.random() * 3);
			event.getDrops().add(bones(random,levelEnchant));

			random = (int) (Math.random() * 2.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));

		} else if (et == EntityType.PIG) {

			int random = (int) (Math.random() * 3);
			event.getDrops().add(bones(random,levelEnchant));

			random = (int) (Math.random() * 2.5 + 0.5);
			event.getDrops().add(animalSkin(random,levelEnchant));

		} else if (et == EntityType.CHICKEN || et == EntityType.RABBIT || et == EntityType.BAT
				|| et == EntityType.PARROT) {

			if (et == EntityType.CHICKEN || et == EntityType.PARROT) {
				ItemStack is = new ItemStack(Material.FEATHER, 1);
				event.getDrops().add(is);
			}
			event.getDrops().add(bones(1,levelEnchant));
			if (et == EntityType.BAT) {
				event.getDrops().add(kno3(1,levelEnchant));
			}
		} else if (et == EntityType.SPIDER || et == EntityType.CAVE_SPIDER) {

			// removing eyes
			event.getDrops().clear();
			ItemStack is = new ItemStack(Material.STRING, rand.nextInt(3)); // 0-2
			event.getDrops().add(is);
		}
	}
}
