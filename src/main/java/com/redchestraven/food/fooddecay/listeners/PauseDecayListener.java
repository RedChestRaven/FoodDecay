package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import com.redchestraven.food.fooddecay.handlers.DecayPauseHandler;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PauseDecayListener implements Listener
{
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static PauseDecayListener _pauseDecayListener = null;
	private final List<Material> containers = List.of(Material.BARREL, Material.BLAST_FURNACE, Material.CHEST, Material.DISPENSER,
			Material.DROPPER, Material.FURNACE, Material.HOPPER, Material.SHULKER_BOX, Material.SMOKER,
			Material.TRAPPED_CHEST,	Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
			Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
			Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
			Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
			Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX);
	private static final List<InventoryAction> _putInInvActions = List.of(InventoryAction.HOTBAR_SWAP,
			InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
			InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.HOTBAR_MOVE_AND_READD);

	private PauseDecayListener()
	{	}

	public static PauseDecayListener GetInstance()
	{
		if(_pauseDecayListener == null)
			_pauseDecayListener = new PauseDecayListener();

		return _pauseDecayListener;
	}

	@EventHandler
	public void OnFoodPutIntoInventoryWithClick(InventoryClickEvent ice)
	{
		ItemStack food = ice.getCursor();

		InventoryType inventoryType = ice.getClickedInventory().getType();
		if(FoodDecay._enabled && _putInInvActions.contains(ice.getAction())
			&& food.getItemMeta().getPersistentDataContainer().has(CustomDataKeys.expirationDate, PersistentDataType.STRING)
			&& (containers.contains(inventoryType) || inventoryType == InventoryType.PLAYER))
		{
			logger.info("Food found on click, toggling paused time!");
			DecayPauseHandler.TogglePausedTime(ice.getClickedInventory(), food);
		}
	}

	@EventHandler
	public void OnFoodPutIntoInventoryWithDrag(InventoryDragEvent ide)
	{
		Map<Integer,ItemStack> foods = ide.getNewItems();
		ItemStack foodToCheck = foods.get(foods.keySet().toArray()[0]);
		InventoryType inventoryType = ide.getInventory().getType();
		if(FoodDecay._enabled
			&& foodToCheck.getItemMeta().getPersistentDataContainer().has(CustomDataKeys.expirationDate, PersistentDataType.STRING)
			&& (containers.contains(inventoryType) || inventoryType == InventoryType.PLAYER))
		{
			logger.info("Food found on drag, toggling paused time!");
			for(ItemStack food : foods.values())
				DecayPauseHandler.TogglePausedTime(ide.getInventory(), food);
		}
	}
}
