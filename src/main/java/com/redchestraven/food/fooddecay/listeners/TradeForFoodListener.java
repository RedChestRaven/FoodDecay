package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.handlers.DecayFoodHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public final class TradeForFoodListener implements Listener
{
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static TradeForFoodListener _tradeForFoodListener = null;
	private static final List<InventoryAction> _pickupActions = List.of(InventoryAction.HOTBAR_SWAP, InventoryAction.PICKUP_ALL,
			InventoryAction.PICKUP_HALF, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY);
	private static final CustomDataKeys cdk = new CustomDataKeys();

	private TradeForFoodListener()
	{	}

	public static TradeForFoodListener GetInstance()
	{
		if(_tradeForFoodListener == null)
			_tradeForFoodListener = new TradeForFoodListener();

		return _tradeForFoodListener;
	}

	@EventHandler
	public void OnTradeForFood(InventoryClickEvent ice)
	{
		if(FoodDecay._enabled)
		{
			Inventory inventory = ice.getClickedInventory();

			// If the inventory is null, it should be a Merchant created by a plugin, instead of a vanilla villager.
			if(inventory != null && inventory.getType() == InventoryType.MERCHANT
					&& ice.getSlotType() == InventoryType.SlotType.RESULT
					&& (_pickupActions.contains(ice.getAction()) || ice.getClick() == ClickType.DROP))
			{
				logger.info("Click used: " + ice.getAction());
				ItemStack result = new ItemStack(ice.getCurrentItem());
				DecayFoodHandler.AddDecayTimeIfDecayingFood(result);

				//The above already handles if it's a regular click, so the code below is purely for when it's a shift-click
				if(result.getItemMeta().getPersistentDataContainer().has(cdk.expirationDate, PersistentDataType.STRING))
				{
					logger.info("Trading to get food, continuing...");
					if (ice.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)
					{
						logger.info("Multi trade, so special handling...");
						ice.setCancelled(true);
						MerchantInventory tradeInventory = (MerchantInventory) ice.getClickedInventory();
						int resultSize = result.getAmount();
						ItemStack[] ingredients = tradeInventory.getContents();
						int tradeRecipeId = FindTradeRecipeUsedId(result, tradeInventory);
						if (tradeRecipeId > -1)
						{
							Merchant merchant = tradeInventory.getMerchant();
							MerchantRecipe tradeRecipe = merchant.getRecipe(tradeRecipeId);
							int usesLeft = tradeRecipe.getMaxUses() - tradeRecipe.getUses();
							logger.info("TradeRecipe found! It has " + usesLeft + " uses left.");
							if(usesLeft > 0)
							{
								logger.info("TradeRecipe has " + usesLeft + " uses left.");
								int resultMaxStackSize = result.getMaxStackSize();
								// The second ingredient is AIR if it's not needed
								boolean isOneItemTrade = tradeRecipe.getIngredients().get(1).getType().equals(Material.AIR);
									logger.info("This is a " + (isOneItemTrade ? "one item" : "two items") + " trade.");
								int maxTrades = (int) Math.floor(ingredients[0].getAmount() / tradeRecipe.getAdjustedIngredient1().getAmount());
								if(!isOneItemTrade)
									maxTrades = Math.min(maxTrades, ingredients[1].getAmount() / tradeRecipe.getIngredients().get(1).getAmount());
								maxTrades = Math.min(maxTrades, usesLeft);

								logger.warning("ResultSize: " + resultSize + " | MaxTrades: " + maxTrades + " | ResultMaxStackSize: " + resultMaxStackSize);
								final int resultTotalTraded = resultSize * maxTrades;
								int maxStacksTraded = (int) Math.floor(resultTotalTraded / resultMaxStackSize)
										+ ((resultTotalTraded % resultMaxStackSize > 0) ? 1 : 0);

								ItemStack[] results = new ItemStack[maxStacksTraded];
								result.setAmount(result.getMaxStackSize());
								for (int i = 0; i < maxStacksTraded - 1; i++)
								{
									logger.info("Full stack number " + i);
									results[i] = new ItemStack(result);
								}
								// This is to accommodate for the possibility of a non-full stack being left over
								if (resultTotalTraded % resultMaxStackSize > 0)
								{
									result.setAmount(resultTotalTraded - ((maxStacksTraded - 1) * resultMaxStackSize));
									logger.warning("Non-full stack! Size is " + result.getAmount());
								}
								else
								{
									logger.warning("Last stack is a full one! " + result.getAmount());
								}
								results[maxStacksTraded - 1] = new ItemStack(result);

								HashMap<Integer, ItemStack> remainder = ice.getWhoClicked().getInventory().addItem(results);
								for (ItemStack rr : remainder.values())
								{
									logger.info("Remainder contains: " + rr.getAmount());
								}
								if (remainder.isEmpty())
								{
									logger.info("All were traded, removing used ingredients...");
									ingredients[0].setAmount(ingredients[0].getAmount() - tradeRecipe.getAdjustedIngredient1().getAmount() * maxTrades);
									if(!isOneItemTrade)
										ingredients[1].setAmount(ingredients[1].getAmount() - tradeRecipe.getIngredients().get(1).getAmount() * maxTrades);
									logger.info("Trades left: " + usesLeft + " | Trades performed: " + maxTrades);
									tradeRecipe.setUses(tradeRecipe.getUses() + maxTrades);
									merchant.setRecipe(tradeRecipeId, tradeRecipe);
								}
								else
								{
									logger.info("More food can be traded than fits!");
									int remainingAmount = 0;
									for (ItemStack partOfRemainder : remainder.values())
										remainingAmount += partOfRemainder.getAmount();

									int excessFullTradeCount = (int) Math.floor(remainingAmount / resultSize);
									if (remainingAmount % resultSize == 0)
									{
										logger.info("Traded remainder exactly fits multiple of a single craft result, changing matrix accordingly...");
										ingredients[0].setAmount(ingredients[0].getAmount() - tradeRecipe.getAdjustedIngredient1().getAmount() * (maxTrades - excessFullTradeCount));
										if(!isOneItemTrade)
											ingredients[1].setAmount(ingredients[1].getAmount() - tradeRecipe.getIngredients().get(1).getAmount() * (maxTrades - excessFullTradeCount));

										logger.info("Trades left: " + usesLeft + " | Trades performed: " + (maxTrades - excessFullTradeCount));
										tradeRecipe.setUses(tradeRecipe.getUses() + maxTrades - excessFullTradeCount);
										merchant.setRecipe(tradeRecipeId, tradeRecipe);
									}
									else
									{
										logger.info("Traded remainder isn't a multiple of a single craft result, changing matrix accordingly...");
										ingredients[0].setAmount(ingredients[0].getAmount() - tradeRecipe.getAdjustedIngredient1().getAmount() * (maxTrades - excessFullTradeCount - 1));
										if(!isOneItemTrade)
											ingredients[1].setAmount(ingredients[1].getAmount() - tradeRecipe.getIngredients().get(1).getAmount() * (maxTrades - excessFullTradeCount - 1));

										logger.info("Trades left: " + usesLeft + " | Trades performed: " + (maxTrades - excessFullTradeCount - 1));
										tradeRecipe.setUses(tradeRecipe.getUses() + maxTrades - excessFullTradeCount - 1);
										merchant.setRecipe(tradeRecipeId, tradeRecipe);

										logger.info("Finishing up with removing the excess items...");
										Inventory playerInv = ice.getWhoClicked().getInventory();
										HashMap<Integer, ItemStack> tradedFoodsMap = (HashMap<Integer, ItemStack>) playerInv.all(result.getType());
										ItemStack[] tradedFoods = tradedFoodsMap.values().toArray(new ItemStack[0]);
										int keyToEdit = -1;

										for (int i = 0; i < tradedFoods.length; i++)
										{
											ItemStack stack = tradedFoods[i];
											PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
											if (stack.getAmount() > (resultSize - remainingAmount)
													&& pdc.has(cdk.expirationDate, PersistentDataType.STRING)
													&& pdc.get(cdk.expirationDate, PersistentDataType.STRING).equals(
													result.getItemMeta().getPersistentDataContainer().get(cdk.expirationDate, PersistentDataType.STRING)))
											{
												logger.info("Found a stack of crafted item with size " + stack.getAmount());
												keyToEdit = (int) tradedFoodsMap.keySet().toArray()[i];
												break;
											}
										}

										if (keyToEdit > -1)
										{
											logger.info("Actually removing the excess items...");
											ItemStack food = tradedFoodsMap.get(keyToEdit);
											food.setAmount(food.getAmount() - (resultSize - (remainingAmount % resultSize)));
											playerInv.setItem(keyToEdit, food);
										}
										else
										{
											logger.warning("Couldn't find any crafted foods with correct timestamp!");
										}
									}
								}

								// If all trades are done, ensure the result is empty too
								if(tradeRecipe.getMaxUses() == tradeRecipe.getUses())
									ingredients[2] = new ItemStack(Material.AIR);
								tradeInventory.setContents(ingredients);
							}
							else
							{
								logger.info("Recipe has already been used up, wait for restock.");
							}
						}
						else
						{
							logger.warning("Trading with a non-standard merchant, not implemented yet!");
						}
					}
					else
					{
						logger.info("Single trade, so regular handling...");
						DecayFoodHandler.AddDecayTimeIfDecayingFood(ice.getCurrentItem());
					}
				}

				logger.info("Done handling trade");
			}
		}
	}

	private int FindTradeRecipeUsedId(ItemStack result, MerchantInventory tradeInventory)
	{
		ItemStack[] offeredIngredients = tradeInventory.getContents();
		// Set the second ingredient to Air if it's empty, to match how merchant recipes are stored.
		// This also ensures I can always compare on Type instead of having to build in more null checks.
		if(offeredIngredients[1] == null) { offeredIngredients[1] = new ItemStack(Material.AIR, 1); }
		Merchant merchant = tradeInventory.getMerchant();

		//logger.warning("Current in Trader: Result " + result.getType()
		//					+ " | Ingredient 1 " + offeredIngredients[0].getType()
		//					+ " | Ingredient 2 " + offeredIngredients[1].getType());
		for(int i = 0; i < merchant.getRecipeCount(); i++)
		{
			MerchantRecipe tradeRecipe = merchant.getRecipe(i);
			ItemStack[] recipeIngredients = tradeRecipe.getIngredients().toArray(new ItemStack[0]);
			//logger.warning("Checking tradeRecipe: Result " + tradeRecipe.getResult().getType()
			//					+ " | Ingredient 1 " + recipeIngredients[0].getType()
			//					+ " | Ingredient 2 " + recipeIngredients[1].getType());
			if(tradeRecipe.getResult().getType().equals(result.getType())
					&& recipeIngredients[0].getType().equals(offeredIngredients[0].getType())
					&& recipeIngredients[1].getType().equals(offeredIngredients[1].getType()))
			{
				return i;
			}
		}

		// Return null if no matching recipe has been found.
		return -1;
	}
}
