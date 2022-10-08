package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.DecayHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.EventNames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ObtainFoodListener implements Listener
{
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static ObtainFoodListener _obtainFoodListener = null;
	private static DecayHandler _decayHandler;
	private static final Map<String, Boolean> _enabledEvents = new HashMap<>();
	private static final List<InventoryAction> _pickupActions = List.of(InventoryAction.HOTBAR_SWAP, InventoryAction.PICKUP_ALL,
			InventoryAction.PICKUP_HALF, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY);

	private ObtainFoodListener(JavaPlugin plugin)
	{
		_decayHandler = DecayHandler.GetInstance(plugin);
		UpdateConfig(plugin.getConfig());
	}

	public static ObtainFoodListener GetInstance(JavaPlugin plugin)
	{
		if(_obtainFoodListener == null)
			_obtainFoodListener = new ObtainFoodListener(plugin);

		return _obtainFoodListener;
	}

	public static void UpdateConfig(FileConfiguration config)
	{
		//logger.info("Events after reload: " + _enabledEvents.get(EventNames.onPickupByPlayer)
		//				+ " | "	+ _enabledEvents.get(EventNames.onPickupByHopper)
		//				+ " | "	+ _enabledEvents.get(EventNames.onMoveToOtherInventory)
		//				+ " | " + _enabledEvents.get(EventNames.onPlayerPickupFromOtherInventory));

		_enabledEvents.clear();
		_enabledEvents.put(EventNames.onPickupByPlayer, config.getBoolean(EventNames.onPickupByPlayer));
		_enabledEvents.put(EventNames.onPickupByHopper, config.getBoolean(EventNames.onPickupByHopper));
		_enabledEvents.put(EventNames.onNonPlayerMoveToOtherInventory, config.getBoolean(EventNames.onNonPlayerMoveToOtherInventory));
		_enabledEvents.put(EventNames.onPlayerPickupFromOtherInventory, config.getBoolean(EventNames.onPlayerPickupFromOtherInventory));

		//logger.info("Events after reload: " + _enabledEvents.get(EventNames.onPickupByPlayer)
		//		+ " | "	+ _enabledEvents.get(EventNames.onPickupByHopper)
		//		+ " | "	+ _enabledEvents.get(EventNames.onMoveToOtherInventory)
		//		+ " | " + _enabledEvents.get(EventNames.onPlayerPickupFromOtherInventory));
	}

	@EventHandler
	public void OnPlayerPickupFood(EntityPickupItemEvent epie)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onPickupByPlayer) && epie.getEntity() instanceof Player)
		{
			ItemStack droppedItemStack = epie.getItem().getItemStack();
			_decayHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
		}
	}

	@EventHandler
	public void OnHopperPickupFood(InventoryPickupItemEvent ipie)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onPickupByHopper))
		{
			ItemStack droppedItemStack = ipie.getItem().getItemStack();
			_decayHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
		}
	}

	@EventHandler
	public void OnNonPlayerMoveToOtherInventory(InventoryMoveItemEvent imie)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onNonPlayerMoveToOtherInventory)
				&& !imie.getSource().equals(imie.getDestination()))
		{
			ItemStack movedItemStack = imie.getItem();
			_decayHandler.AddDecayTimeIfDecayingFood(movedItemStack);
		}
	}

	@EventHandler
	public void OnPlayerPickupFromOtherInventory(InventoryClickEvent ice)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onPlayerPickupFromOtherInventory))
		{
			Inventory topInventory = ice.getClickedInventory();
			if(topInventory != null && topInventory.getType() != InventoryType.PLAYER
					&& _pickupActions.contains(ice.getAction())
					&& ice.getCurrentItem() != null)
			{
				_decayHandler.AddDecayTimeIfDecayingFood(ice.getCurrentItem());
			}
		}
	}
}