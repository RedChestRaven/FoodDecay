package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.ContainerGroups;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import com.redchestraven.food.fooddecay.consts.InvActionGroups;
import com.redchestraven.food.fooddecay.handlers.DecayPauseHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.logging.Logger;

public class PauseDecayListener implements Listener
{
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static PauseDecayListener _pauseDecayListener = null;

	private PauseDecayListener()
	{	}

	public static PauseDecayListener GetInstance()
	{
		if (_pauseDecayListener == null)
			_pauseDecayListener = new PauseDecayListener();

		return _pauseDecayListener;
	}

	@EventHandler
	public void OnFoodPutIntoInventoryWithClick(InventoryClickEvent ice)
	{
		ItemStack food = ice.getCursor();

		InventoryType inventoryType = ice.getClickedInventory().getType();
		if (FoodDecay._enabled && InvActionGroups.pausePutInInvActions.contains(ice.getAction())
			&& food.getItemMeta().getPersistentDataContainer().has(CustomDataKeys.expirationDate, PersistentDataType.STRING)
			&& (ContainerGroups.containers.contains(inventoryType) || inventoryType == InventoryType.PLAYER))
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
		if (FoodDecay._enabled
			&& foodToCheck.getItemMeta().getPersistentDataContainer().has(CustomDataKeys.expirationDate, PersistentDataType.STRING)
			&& (ContainerGroups.containers.contains(inventoryType) || inventoryType == InventoryType.PLAYER))
		{
			logger.info("Food found on drag, toggling paused time!");
			for (ItemStack food : foods.values())
				DecayPauseHandler.TogglePausedTime(ide.getInventory(), food);
		}
	}
}
