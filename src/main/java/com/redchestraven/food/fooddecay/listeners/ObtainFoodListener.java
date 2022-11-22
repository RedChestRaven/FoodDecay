package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.handlers.DecayFoodHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
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
	private static DecayFoodHandler _decayFoodHandler;
	private static final List<InventoryAction> _pickupActions = List.of(InventoryAction.HOTBAR_SWAP, InventoryAction.PICKUP_ALL,
			InventoryAction.PICKUP_HALF, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY,
			InventoryAction.HOTBAR_MOVE_AND_READD, InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT);
	private static final CustomDataKeys cdk = new CustomDataKeys();

	private ObtainFoodListener(JavaPlugin plugin)
	{
		_decayFoodHandler = DecayFoodHandler.GetInstance(plugin);
	}

	public static ObtainFoodListener GetInstance(JavaPlugin plugin)
	{
		if(_obtainFoodListener == null)
			_obtainFoodListener = new ObtainFoodListener(plugin);

		return _obtainFoodListener;
	}

	@EventHandler
	public void OnPlayerPickupFood(EntityPickupItemEvent epie)
	{
		if(FoodDecay._enabled && epie.getEntity() instanceof Player)
		{
			ItemStack droppedItemStack = epie.getItem().getItemStack();
			_decayFoodHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
		}
	}

	@EventHandler
	public void OnHopperPickupFood(InventoryPickupItemEvent ipie)
	{
		if(FoodDecay._enabled)
		{
			ItemStack droppedItemStack = ipie.getItem().getItemStack();
			_decayFoodHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
		}
	}

	@EventHandler
	public void OnNonPlayerMoveToOtherInventory(InventoryMoveItemEvent imie)
	{
		if(FoodDecay._enabled && !imie.getSource().equals(imie.getDestination()))
		{
			ItemStack movedItemStack = imie.getItem();
			_decayFoodHandler.AddDecayTimeIfDecayingFood(movedItemStack);
		}
	}

	@EventHandler
	public void OnPlayerPickupFromOtherInventory(InventoryClickEvent ice)
	{
		if(FoodDecay._enabled)
		{
			logger.info("Pickup action is " + ice.getAction());
			Inventory topInventory = ice.getClickedInventory();
			if(topInventory != null && topInventory.getType() != InventoryType.PLAYER
					&& ice.getSlotType() == InventoryType.SlotType.CONTAINER
					&& _pickupActions.contains(ice.getAction())
					&& ice.getCurrentItem() != null)
			{
				_decayFoodHandler.AddDecayTimeIfDecayingFood(ice.getCurrentItem());
			}
		}
	}

	@EventHandler
	public void OnCraftingFood(CraftItemEvent cie)
	{
		if(FoodDecay._enabled)
		{
			logger.info("Craft triggered!");
			logger.info("Click used: " + cie.getAction());

			// Case of shift click, trying to make all possible from the crafting grid, and put them in the inventory
			// myself, while cancelling the actual craft event
			if(cie.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
			{
				logger.info("Multi craft, so special handling...");
				ItemStack result = cie.getRecipe().getResult();
				int resultSize = result.getAmount();

				_decayFoodHandler.AddDecayTimeIfDecayingFood(result);
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
					final int resultTotalCrafted = maxTimesCraftable * resultSize;
					int amountOfStacksCrafted = (int) Math.floor(1f * resultTotalCrafted / resultMaxStackSize)
													+ (resultTotalCrafted % resultMaxStackSize) > 0 ? 1 : 0 ;
					ItemStack[] results = new ItemStack[amountOfStacksCrafted];
					result.setAmount(result.getMaxStackSize());
					for(int i = 0; i < amountOfStacksCrafted - 1; i++)
					{
						logger.info("Full stack number " + i);
						results[i] = new ItemStack(result);
					}
					// This is to accommodate for the possibility of a non-full stack being left over
					if(resultTotalCrafted % resultMaxStackSize > 0)
					{
						result.setAmount(resultTotalCrafted - ((amountOfStacksCrafted - 1) * resultMaxStackSize));
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
							ItemStack[] craftedFoods = craftedFoodsMap.values().toArray(new ItemStack[0]);
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
								food.setAmount(food.getAmount() - (resultSize - (remainingAmount % resultSize)));
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
				_decayFoodHandler.AddDecayTimeIfDecayingFood(cie.getCurrentItem());
			}

			logger.info("Done handling craft");
		}
	}
}