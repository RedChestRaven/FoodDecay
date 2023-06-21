package com.redchestraven.food.fooddecay.handlers;

import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import com.redchestraven.food.fooddecay.consts.Formatters;
import com.redchestraven.food.fooddecay.customtypes.DecayingFoodGroup;
import com.redchestraven.food.fooddecay.helpers.Predicates;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SimpleDecayFoodHandler
{
	private static JavaPlugin _plugin;
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static Predicates _predicates;
	private static int _decayCheckerTaskId = -1;
	private static List<String> _activeWorlds = new ArrayList<>();

	private static final HashSet<DecayingFoodGroup> _decayingFoodGroups = new HashSet<>();
	private static final HashSet<Material> _decayStoppers = new HashSet<>();
	private static ItemStack _rottenFood;
	private static int _decayCheckInterval;

	private SimpleDecayFoodHandler() { }

	public static boolean StartInstance(JavaPlugin plugin)
	{
		_plugin = plugin;
		_predicates = Predicates.GetInstance();

		//Creating rotten food item to replace food with
		_rottenFood = new ItemStack(Material.ROTTEN_FLESH);
		ItemMeta rottenFoodMeta = _rottenFood.getItemMeta();
		rottenFoodMeta.setDisplayName(ChatColor.DARK_RED + "Rotten Food");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.DARK_PURPLE + "Ugh, the smell is horrid...");
		lore.add(ChatColor.DARK_PURPLE + "Maybe this can still be used some way?");
		rottenFoodMeta.setLore(lore);
		_rottenFood.setItemMeta(rottenFoodMeta);

		UpdateConfig(plugin.getConfig()); // This will also start the repeating task

		return true;
	}

	private static void StartRepeatingDecayCheck(JavaPlugin plugin)
	{
		_decayCheckerTaskId = Bukkit.getScheduler().runTaskTimer(plugin,
				() -> { // This is a lambda expression to create an anonymous Runnable.
					if (FoodDecay._enabled)
					{
						// First set up pausing
						// Then set up decay
						//logger.info("Starting simple decay check...");
						Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
						//logger.info("There are " + onlinePlayers.size() + " online players to check.");
						for (Player player : onlinePlayers)
						{
							DecayInventory(player.getInventory());
							DecayInventory(player.getEnderChest());
						}
						//logger.info("Players have been checked, and food has been decayed. Moving on to containers...");

						for (String worldName: _activeWorlds)
						{
							Chunk[] loadedChunks = Bukkit.getWorld(worldName).getLoadedChunks();
							//int totalEntitiesLoaded = 0;
							for (Chunk chunk : loadedChunks)
							{
								if (chunk.isEntitiesLoaded())
								{
									// Looking for blocks with inventories. Not all of these are entities, like the furnace, so I
									// need to use this method for those.
									List<BlockState> containers = Arrays.stream(chunk.getTileEntities()).filter(_predicates.GetContainerChecker()).collect(Collectors.toList());
									//totalEntitiesLoaded += containers.size();
									if (containers.size() > 0)
									{
										//logger.info("There are " + containers.size() + " containers to check.");

										for (BlockState container : containers)
										{
											//logger.info("Looking at a " + container.getType());
											if (container instanceof InventoryHolder)
												DecayInventory(((InventoryHolder) container).getInventory());
											else
												logger.warning("Can't check inventory of Blockstate: " + container.getType());
										}
									}

									// Vehicles, however, are Entities and can be InventoryHolders, but won't have a BlockState,
									// so I'm getting them this way.
									Object[] entities = Arrays.stream(chunk.getEntities()).filter(_predicates.GetStorageEntityChecker()).toArray();
									//totalEntitiesLoaded += entities.length;
									//logger.info("There are " + entities.length + " unchecked entities to check.");

									for (Object entity : entities)
									{
										if (entity instanceof InventoryHolder)
											DecayInventory(((InventoryHolder) entity).getInventory());
									}
								}
							}
							//logger.info("A total of " + totalEntitiesLoaded + " containers have been checked in " + loadedChunks.length + " loaded chunks in world " + worldName + " , and food has been decayed.");
						}
						//logger.info("Simple decay check finished.");
					}
				},
				(_decayCheckInterval * 20L), // In server ticks. 1 second = 20 server ticks
				(_decayCheckInterval * 20L) // In server ticks. 1 second = 20 server ticks
		).getTaskId();
	}

	public static void UpdateConfig(@NotNull FileConfiguration config)
	{
		_decayingFoodGroups.clear();
		ConfigurationSection decayingFoodGroups = config.getConfigurationSection(ConfigSettingNames.decayingFoodGroups);
		for (String decayingFoodGroupName: decayingFoodGroups.getKeys(false))
		{
			int decayingFoodGroupRateOfDecay = decayingFoodGroups.getInt(decayingFoodGroupName + "." + ConfigSettingNames.rateOfDecay);
			ArrayList<Material> decayingFoods = new ArrayList<>();
			for (String decayingFoodName : decayingFoodGroups.getStringList(decayingFoodGroupName + "." + ConfigSettingNames.decayingFoods))
			{
				// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
				decayingFoods.add(Material.getMaterial(decayingFoodName.toUpperCase().replace(' ', '_')));
			}
			_decayingFoodGroups.add(new DecayingFoodGroup(decayingFoodGroupName, decayingFoodGroupRateOfDecay, decayingFoods));
		}

		_decayStoppers.clear();
		for (String decayStopperName: config.getStringList(ConfigSettingNames.decayStoppers))
		{
			// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
			_decayStoppers.add(Material.getMaterial(decayStopperName.toUpperCase().replace(' ', '_')));
		}

		_decayCheckInterval = config.getInt(ConfigSettingNames.decayCheckInterval);

		_activeWorlds = config.getStringList(ConfigSettingNames.worlds);

		if (_decayCheckerTaskId != -1) Bukkit.getScheduler().cancelTask(_decayCheckerTaskId);
		StartRepeatingDecayCheck(_plugin);
	}

	private static void DecayInventory(Inventory inventory)
	{
		for (Material _decayStopper : _decayStoppers)
		{
			if (_decayStopper != null && inventory.contains(_decayStopper))
			{
				//logger.info("Food is put on ice, so pausing decay...");
				PauseDecayInInventory(inventory);
				return;
			}
		}

		for (DecayingFoodGroup decayingFoodGroup: _decayingFoodGroups)
		{
			for (Material _decayingFood: decayingFoodGroup.GetDecayingFoods())
			{
				if (_decayingFood != null && inventory.contains(_decayingFood))
				{
					//logger.info(String.format("Container has %s in their inventory!", _decayingFood));
					HashMap<Integer, ? extends ItemStack> decayingFoodStacks = inventory.all(_decayingFood);

					// Using the keyset instead of the values, since I might need to replace the item at its slotindex
					for (int decayingFoodStackKey : decayingFoodStacks.keySet())
					{
						ItemStack decayingFoodStack = decayingFoodStacks.get(decayingFoodStackKey);
						ItemMeta decayingFoodStackMeta = decayingFoodStack.getItemMeta();
						PersistentDataContainer decayingFoodStackPdc = decayingFoodStackMeta.getPersistentDataContainer();

						if (decayingFoodStackPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
						{
							logger.info("Applying PausedTimeLeft to expiration date..." + decayingFoodStackPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));
							String[] splitPausedTimeLeft = decayingFoodStackPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING).split("-");
							LocalDateTime newDecayTimestamp = LocalDateTime.now().plusYears(Integer.parseInt(splitPausedTimeLeft[0]))
									.plusMonths(Integer.parseInt(splitPausedTimeLeft[1]))
									.plusDays(Integer.parseInt(splitPausedTimeLeft[2]))
									.plusHours(Integer.parseInt(splitPausedTimeLeft[3]))
									.plusMinutes(Integer.parseInt(splitPausedTimeLeft[4]))
									.plusSeconds(Integer.parseInt(splitPausedTimeLeft[5]));
							logger.warning("New expirationdate: " + newDecayTimestamp.format(Formatters.internalTimeFormat));
							decayingFoodStackPdc.set(CustomDataKeys.expirationDate, PersistentDataType.STRING, newDecayTimestamp.format(Formatters.internalTimeFormat));
							decayingFoodStackPdc.remove(CustomDataKeys.pausedTimeLeft);

							List<String> lore = decayingFoodStackMeta.hasLore() ? decayingFoodStackMeta.getLore() : new ArrayList<>();
							int loreLineToAdjust = -1;
							for (int i = 0; i < lore.size(); i++)
							{
								if (lore.get(i).contains("Will decay at:"))
								{
									loreLineToAdjust = ++i;
									break;
								}
							}
							if (loreLineToAdjust >= 0)
							{
								lore.set(loreLineToAdjust, ChatColor.DARK_GREEN + newDecayTimestamp.format(Formatters.loreTimeFormat));
							}
							else
							{
								lore.add(ChatColor.DARK_GREEN + "Will decay at:");
								lore.add(ChatColor.DARK_GREEN + newDecayTimestamp.format(Formatters.loreTimeFormat));
							}

							decayingFoodStackMeta.setLore(lore);
							decayingFoodStack.setItemMeta(decayingFoodStackMeta);
							logger.info("Expiration date at " + newDecayTimestamp.format(Formatters.loreTimeFormat) + " stored!");
						}
						else
						{
							if (decayingFoodStackPdc.has(CustomDataKeys.expirationDate, PersistentDataType.STRING))
							{
								if (CheckIfTimestampExpired(decayingFoodStackPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING)))
								{
									logger.info("Rotting food found, converting...");
									_rottenFood.setAmount(decayingFoodStack.getAmount());
									inventory.setItem(decayingFoodStackKey, _rottenFood);
									logger.info("Food is now rotten!");
								}
							}
							else
							{
								logger.info("There is no expiration date stored yet...");
								LocalDateTime decayTimestamp = LocalDateTime.now().plusSeconds(decayingFoodGroup.GetRateOfDecay());
								decayingFoodStackPdc.set(CustomDataKeys.expirationDate, PersistentDataType.STRING, decayTimestamp.format(Formatters.internalTimeFormat));

								List<String> lore = decayingFoodStackMeta.hasLore() ? decayingFoodStackMeta.getLore() : new ArrayList<>();
								lore.add(ChatColor.DARK_GREEN + "Will decay at:");
								lore.add(ChatColor.DARK_GREEN + decayTimestamp.format(Formatters.loreTimeFormat));
								decayingFoodStackMeta.setLore(lore);
								decayingFoodStack.setItemMeta(decayingFoodStackMeta);
								logger.info("Expiration date of " + decayingFoodGroup.GetRateOfDecay() + " seconds stored!");
							}
						}
					}
				}
			}
		}
	}

	private static void PauseDecayInInventory(Inventory inventory)
	{
		for (DecayingFoodGroup decayingFoodGroup : _decayingFoodGroups)
		{
			for (Material _decayingFood : decayingFoodGroup.GetDecayingFoods())
			{
				if (_decayingFood != null && inventory.contains(_decayingFood))
				{
					//logger.info(String.format("Container has %s in their inventory!", _decayingFood.name()));
					HashMap<Integer, ? extends ItemStack> similarItemStacks = inventory.all(_decayingFood);

					for (ItemStack foodStack : similarItemStacks.values())
					{
						ItemMeta foodMeta = foodStack.getItemMeta();
						PersistentDataContainer foodPdc = foodMeta.getPersistentDataContainer();

						//logger.info("Starting pause checks... already has paused time: " + foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));
						if (foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
						{
							//logger.info("Food already was paused, skip...");
							continue;
						}

						List<String> lore = foodMeta.hasLore() ? foodMeta.getLore() : new ArrayList<>();
						//logger.info("Lore after test: " + lore);

						if (foodPdc.has(CustomDataKeys.expirationDate, PersistentDataType.STRING))
						{
							logger.info("Food already has an expirationDate, pausing ...");
							String[] decayTimestampStringified = foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING).split("-");
							logger.warning("Setting paused time left before decay...");
							LocalDateTime now = LocalDateTime.now();
							logger.info("Now: " + LocalDateTime.now().format(Formatters.internalTimeFormat));
							logger.info("Time of decay: " + foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING));
							LocalDateTime decayTimestamp = LocalDateTime.of(Integer.parseInt(decayTimestampStringified[0]),
									Integer.parseInt(decayTimestampStringified[1]),
									Integer.parseInt(decayTimestampStringified[2]),
									Integer.parseInt(decayTimestampStringified[3]),
									Integer.parseInt(decayTimestampStringified[4]),
									Integer.parseInt(decayTimestampStringified[5]));
							String pausedTimeLeft = getPausedTimeLeftAsString(now, decayTimestamp);

							logger.info("Paused time left before decay: " + pausedTimeLeft);
							foodPdc.set(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING, pausedTimeLeft);
							logger.info("Paused time left cdk: " + foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));

							int loreLineToAdjust = -1;
							for (int i = 0; i < lore.size(); i++)
							{
								if (lore.get(i).contains("Will decay at:"))
								{
									loreLineToAdjust = ++i;
									break;
								}
							}
							if (loreLineToAdjust >= 0)
								lore.set(loreLineToAdjust, ChatColor.DARK_GREEN + "Decay paused.");
							foodMeta.setLore(lore);
							foodStack.setItemMeta(foodMeta);
							logger.info("Food has been paused.");
							continue;
						}

						// Since there's neither of the custom data keys, it's a new food item that gets to be paused right away!
						//logger.info("Adding full PausedTimeLeft.");
						LocalDateTime now = LocalDateTime.now();
						String pausedTimeLeft = getPausedTimeLeftAsString(now, now.plusSeconds(decayingFoodGroup.GetRateOfDecay()));
						foodPdc.set(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING, pausedTimeLeft);
						//logger.warning("Stored PausedTimeLeft: " + foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));
						lore.add(ChatColor.DARK_GREEN + "Will decay at:");
						lore.add(ChatColor.DARK_GREEN + "Decay paused.");
						foodMeta.setLore(lore);
						foodStack.setItemMeta(foodMeta);
						//logger.info("Food has been paused from the start.");
					}
				}
			}
		}
	}

	@NotNull
	private static String getPausedTimeLeftAsString(LocalDateTime now, LocalDateTime later)
	{
		//logger.info("Received later: " + later.toString());

		long pausedTimeLeft_Years = ChronoUnit.YEARS.between(now, later);
		later = later.minusYears(pausedTimeLeft_Years);
		long pausedTimeLeft_Months = ChronoUnit.MONTHS.between(now, later);
		later = later.minusMonths(pausedTimeLeft_Months);
		long pausedTimeLeft_Days = ChronoUnit.DAYS.between(now, later);
		later = later.minusDays(pausedTimeLeft_Days);
		long pausedTimeLeft_Hours = ChronoUnit.HOURS.between(now, later);
		later = later.minusHours(pausedTimeLeft_Hours);
		long pausedTimeLeft_Minutes = ChronoUnit.MINUTES.between(now, later);
		later = later.minusMinutes(pausedTimeLeft_Minutes);
		long pausedTimeLeft_Seconds = ChronoUnit.SECONDS.between(now, later);

		/*
		logger.warning("PausedTimeLeft using ChronoUnit:"
				+ " Years=" + pausedTimeLeft_Years
				+ " Months=" + pausedTimeLeft_Months
				+ " Days=" + pausedTimeLeft_Days
				+ " Hours=" + pausedTimeLeft_Hours
				+ " Minutes=" + pausedTimeLeft_Minutes
				+ " Seconds=" + pausedTimeLeft_Seconds);
		 */

		String pausedTimeLeft = String.join("-",
				String.format(Formatters.internalPausedTimeFormat[0], pausedTimeLeft_Years),
				String.format(Formatters.internalPausedTimeFormat[1], pausedTimeLeft_Months),
				String.format(Formatters.internalPausedTimeFormat[2], pausedTimeLeft_Days),
				String.format(Formatters.internalPausedTimeFormat[3], pausedTimeLeft_Hours),
				String.format(Formatters.internalPausedTimeFormat[4], pausedTimeLeft_Minutes),
				String.format(Formatters.internalPausedTimeFormat[5], pausedTimeLeft_Seconds));
		return pausedTimeLeft;
	}

	private static boolean CheckIfTimestampExpired(String _foodDecayTimestamp)
	{
		String[] foodDecayTimestamp = _foodDecayTimestamp.split("-");

		LocalDateTime now = LocalDateTime.now();
		logger.info("Now: " + now.format(Formatters.internalTimeFormat));
		LocalDateTime decayTimestamp = LocalDateTime.of(
				Integer.parseInt(foodDecayTimestamp[0]),	//Year
				Integer.parseInt(foodDecayTimestamp[1]),	//Month
				Integer.parseInt(foodDecayTimestamp[2]),	//Day
				Integer.parseInt(foodDecayTimestamp[3]),	//Hour
				Integer.parseInt(foodDecayTimestamp[4]),	//Minute
				Integer.parseInt(foodDecayTimestamp[5]));	//Second
		logger.info("ExpirationTime: " + decayTimestamp.format(Formatters.internalTimeFormat));

		return decayTimestamp.isBefore(now);
	}
}
