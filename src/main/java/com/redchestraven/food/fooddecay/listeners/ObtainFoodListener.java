package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.DecayHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import com.redchestraven.food.fooddecay.consts.EventNames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
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
		_enabledEvents.put(EventNames.onTradeForFood, config.getBoolean(EventNames.onTradeForFood));
		_enabledEvents.put(EventNames.onCraftingFood, config.getBoolean(EventNames.onCraftingFood));

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
					&& ice.getSlotType() == InventoryType.SlotType.CONTAINER
					&& _pickupActions.contains(ice.getAction())
					&& ice.getCurrentItem() != null)
			{
				_decayHandler.AddDecayTimeIfDecayingFood(ice.getCurrentItem());
			}
		}
	}

	@EventHandler
	public void OnTradeForFood(InventoryClickEvent ice)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onTradeForFood))
		{
			Inventory tradeInventory = ice.getClickedInventory();
			if(tradeInventory != null && tradeInventory.getType() == InventoryType.MERCHANT
					&& ice.getSlotType() == InventoryType.SlotType.RESULT
					&& (_pickupActions.contains(ice.getAction()) || ice.getClick() == ClickType.DROP))
			{
				logger.info("Click used: " + ice.getAction());

				_decayHandler.AddDecayTimeIfDecayingFood(ice.getCurrentItem());
			}
		}
	}

	@EventHandler
	public void OnCraftingFood(CraftItemEvent cie)
	{
		if(FoodDecay._enabled && _enabledEvents.get(EventNames.onCraftingFood))
		{
			logger.info("Craft triggered!");

			// Case of shift click, trying to make all possible from the crafting grid, and put them in the inventory
			// myself, while cancelling the actual craft event
			if(cie.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
			{
				logger.info("Multi craft, so special handling...");
				ItemStack result = cie.getRecipe().getResult();
				int resultSize = result.getAmount();
				CustomDataKeys cdk = new CustomDataKeys();

				_decayHandler.AddDecayTimeIfDecayingFood(result);
				if(result.getItemMeta().getPersistentDataContainer().has(cdk.expirationDate, PersistentDataType.STRING))
				{
					logger.info("Crafted item is a decaying food, handling crafting myself...");
					cie.setCancelled(true);
					int maxTimesCraftable = 999;
					ItemStack[] ingredients = cie.getInventory().getMatrix();
					for (ItemStack ingredient : ingredients)
					{
						// This logic is for now assuming ingredients for a recipe are always in stacks of 1.
						if (ingredient != null && ingredient.getAmount() < maxTimesCraftable)
							maxTimesCraftable = ingredient.getAmount();
					}

					// Preparing the crafted food in stacks, to offer it in the right stack size to prevent them being too big
					// Ex.: crafting 7 mushroom stew should take 7 empty slots, not 1 slot as a stack of 7
					int resultMaxStackSize = result.getMaxStackSize();
					int amountOfStacksCrafted = (int) Math.ceil(1f * (maxTimesCraftable * resultSize) / resultMaxStackSize);
					ItemStack[] results = new ItemStack[amountOfStacksCrafted];
					result.setAmount(result.getMaxStackSize());
					for(int i = 0; i < amountOfStacksCrafted - 1; i++)
					{
						logger.info("Full stack number " + i);
						results[i] = new ItemStack(result);
					}
					// This is to accommodate for the possibility of a non-full stack being left over
					if((maxTimesCraftable * resultSize) % resultMaxStackSize > 0)
					{
						result.setAmount((maxTimesCraftable * resultSize) - ((amountOfStacksCrafted - 1) * resultMaxStackSize));
						logger.warning("Non-full stack! Size is " + result.getAmount());
					}
					else
					{
						logger.warning("Last stack is a full one! " + result.getAmount());
					}
					results[amountOfStacksCrafted - 1] = new ItemStack(result);

					for(ItemStack r: results)
					{
						logger.info("Stack size to put in inventory: " + r.getAmount());
					}

					HashMap<Integer, ItemStack> remainder = cie.getWhoClicked().getInventory().addItem(results);
					for(ItemStack rr: remainder.values())
					{
						logger.info("Remainder contains: " + rr.getAmount());
					}
					if(remainder.isEmpty())
					{
						logger.info("All were crafted, clearing crafting inventory as far as needed...");
						for (ItemStack ingredient : ingredients)
						{
							if (ingredient != null)
								ingredient.setAmount(ingredient.getAmount() - maxTimesCraftable);
						}
						cie.getInventory().setMatrix(ingredients);
					}
					else
					{
						logger.warning("More food can be crafted than fits!");

						int remainingAmount = 0;
						for(ItemStack partOfRemainder: remainder.values())
						{
							remainingAmount += partOfRemainder.getAmount();
						}

						int excessFullCraftCount = (int) Math.floor(1f * remainingAmount / resultSize);
						if (remainingAmount % resultSize == 0)
						{
							logger.info("Crafted remainder exactly fits multiple of a single craft result, changing matrix accordingly...");
							for (ItemStack ingredient : ingredients)
							{
								if (ingredient != null)
									ingredient.setAmount(ingredient.getAmount() - maxTimesCraftable + excessFullCraftCount);
							}
						}
						else
						{
							logger.info("Crafted remainder isn't a multiple of a single craft result, changing matrix accordingly...");
							for (ItemStack ingredient : ingredients)
							{
								if (ingredient != null)
									ingredient.setAmount(ingredient.getAmount() - maxTimesCraftable + excessFullCraftCount + 1);
							}

							logger.info("Finishing up with removing the excess items...");
							Inventory playerInv = cie.getWhoClicked().getInventory();
							HashMap<Integer, ItemStack> craftedFoodsMap = (HashMap<Integer, ItemStack>) playerInv.all(result.getType());
							ItemStack[] craftedFoods = (ItemStack[]) craftedFoodsMap.values().toArray();
							int keyToEdit = -1;

							for (int i = 0; i < craftedFoods.length; i++)
							{
								ItemStack stack = craftedFoods[i];
								PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
								if (stack.getAmount() > (resultSize - remainingAmount)
										&& pdc.has(cdk.expirationDate, PersistentDataType.STRING)
										&& pdc.get(cdk.expirationDate, PersistentDataType.STRING).equals(
										result.getItemMeta().getPersistentDataContainer().get(cdk.expirationDate, PersistentDataType.STRING)))
								{
									logger.info("Found a stack of crafted item...");
									keyToEdit = (int) craftedFoodsMap.keySet().toArray()[i];
									break;
								}
							}

							if (keyToEdit > -1)
							{
								logger.info("Actually removing the excess items...");
								ItemStack food = craftedFoodsMap.get(keyToEdit);
								food.setAmount(food.getAmount() - (resultSize - remainingAmount));
								playerInv.setItem(keyToEdit, food);
							} else
							{
								logger.warning("Couldn't find any crafted foods with correct timestamp!");
							}
						}

						cie.getInventory().setMatrix(ingredients);
					}
				}
			}
			else
			{
				logger.info("Single craft, so regular handling...");
				_decayHandler.AddDecayTimeIfDecayingFood(cie.getCurrentItem());
			}

			logger.info("Done handling craft");
		}
	}
}