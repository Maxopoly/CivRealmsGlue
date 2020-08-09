package com.civrealms.crgpve;

/**
 * @author Crimeo
 */

import com.civrealms.crgmain.CivRealmsGlue;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.block.Container;
import org.bukkit.block.Block;

public class ContrabandListener implements Listener {
	// to remove items from inventories that shouldn't be there
	// currently, this focuses just on things that are likely to show up in dungeons
	// etc.
	// enchanted books, various armors and tools, and some things like gold bars
	// without lore

	private CivRealmsGlue plugin;
	public static Logger LOG = Logger.getLogger("CivRealmsGlue");

	public ContrabandListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}

	Set<Material> armor = EnumSet.of(Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_BOOTS,
			Material.GOLD_LEGGINGS, Material.GOLD_BARDING, Material.IRON_HELMET, Material.IRON_CHESTPLATE,
			Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_BARDING, Material.DIAMOND_HELMET,
			Material.DIAMOND_CHESTPLATE, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BARDING,
			Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
			Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_BOOTS,
			Material.CHAINMAIL_LEGGINGS);

	Set<Material> tools = EnumSet.of(Material.GOLD_AXE, Material.GOLD_PICKAXE, Material.GOLD_SPADE, Material.GOLD_HOE,
			Material.GOLD_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE, Material.IRON_SPADE, Material.IRON_HOE,
			Material.IRON_SWORD, Material.DIAMOND_AXE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SPADE,
			Material.DIAMOND_HOE, Material.DIAMOND_SWORD, Material.STONE_AXE, Material.STONE_PICKAXE,
			Material.STONE_SPADE, Material.STONE_HOE, Material.STONE_SWORD);

	// 1) opening inventories
	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if ((event.getInventory().getSize() == 27) && !event.getInventory().getTitle().toLowerCase().contains("chest") ) return;

		if (event.getInventory().getContents().length > 0) {
			checkContraband(event.getInventory().getContents());
		}
		
	}

	// 2) Breaking inventory blocks
	@EventHandler(priority = EventPriority.LOW)
	public void onContainerBreak(BlockBreakEvent event) {
		if (event.getBlock().getState() instanceof InventoryHolder) {
			Container container = (Container) event.getBlock().getState();
			if (container.getInventory().getContents().length > 0) {
				checkContraband(container.getInventory().getContents());
			}
		}
	}

	// 3) Exploding inventories
	@EventHandler(priority = EventPriority.LOW)
	public void onContainerExplode(EntityExplodeEvent event) {
		for (Block b : event.blockList()) { // blocks exploded BY the exploding block, not the exploding block
			if (b.getState() instanceof InventoryHolder) {
				Container container = (Container) b.getState();
				if (container.getInventory().getContents().length > 0) {
					checkContraband(container.getInventory().getContents());
				}
			}
		}
	}

	// 4) hoppers, hopper carts, etc.
	@EventHandler(priority = EventPriority.LOW)
	public void onItemMove(InventoryMoveItemEvent event) {
		ItemStack[] isa = new ItemStack[1];
		isa[0] = event.getItem();
		if (isa.length > 0) {
			checkContraband(isa);
		}
	}

	private boolean checkContraband(ItemStack[] isa) { // true if item is contraband
		for (ItemStack is : isa) {
			if (is != null) {
				if (armor.contains(is.getType())) {
					if (!is.getItemMeta().hasLore()) {
						is.setAmount(0);
					}
				} else if (tools.contains(is.getType())) {
					if (!is.getItemMeta().hasLore()) {
						is.setAmount(0);
					}
				} else if (is.getType() == Material.ENCHANTED_BOOK) {
					is.setAmount(0);
				} else if (is.getType() == Material.ENDER_PEARL || is.getType() == Material.GOLD_INGOT
						|| is.getType() == Material.ROTTEN_FLESH) {
					if (!is.getItemMeta().hasLore()) {
						is.setAmount(0);
					}
				}
			}
		}
		return false;
	}
}
