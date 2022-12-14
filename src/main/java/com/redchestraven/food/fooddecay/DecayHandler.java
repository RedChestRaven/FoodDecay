package com.redchestraven.food.fooddecay;

import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class DecayHandler implements Listener
{
	private static DecayHandler _decayHandler = null;
	private static JavaPlugin _plugin;
	private static final CustomDataKeys _customDataKeys = new CustomDataKeys();
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static final DateFormat _timeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	private static ContainerChecker _containerChecker;
	private static StorageEntityChecker _storageEntityChecker;
	private static int _decayCheckerTaskId = -1;

	private static final HashSet<Material> _decayingFoods = new HashSet<>();
	private static final HashSet<Material> _decayStoppers = new HashSet<>();
	private static ItemStack _rottenFood;
	private static int _rateOfDecay;
	private static int _decayCheckInterval;

	private DecayHandler(JavaPlugin plugin)
	{
		_plugin = plugin;
		_containerChecker = new ContainerChecker();
		_storageEntityChecker = new StorageEntityChecker();
		UpdateConfig(plugin.getConfig()); // This will also start the repeating task

		//Creating rotten food item to replace food with
		_rottenFood = new ItemStack(Material.ROTTEN_FLESH);
		ItemMeta rottenFoodMeta = _rottenFood.getItemMeta();
		rottenFoodMeta.setDisplayName(ChatColor.DARK_RED + "Rotten Food");
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.DARK_PURPLE + "Ugh, the smell is horrid...");
		lore.add(ChatColor.DARK_PURPLE + "Maybe this can still be used some way?");
		rottenFoodMeta.setLore(lore);
		_rottenFood.setItemMeta(rottenFoodMeta);
	}

	private static void StartRepeatingDecayCheck(JavaPlugin plugin)
	{
		_decayCheckerTaskId = Bukkit.getScheduler().runTaskTimer(plugin,
				() -> { // This is a lambda expression to create an anonymous Runnable.
					if(FoodDecay._enabled)
					{
						Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
						//logger.info("There are " + onlinePlayers.size() + " online players to check.");
						for (Player player : onlinePlayers)
						{
							DecayInventory(player.getInventory());
							DecayInventory(player.getEnderChest());
						}
						//logger.info("Players have been checked, and food has been decayed. Moving on to containers...");

						Chunk[] loadedChunks = Bukkit.getWorld("testing").getLoadedChunks();
						int totalEntitiesLoaded = 0;
						for(Chunk chunk: loadedChunks)
						{
							if(chunk.isEntitiesLoaded())
							{
								// Looking for blocks with inventories. Not all of these are entities, like the furnace, so I
								// need to use this method for those.
								List<BlockState> containers = Arrays.stream(chunk.getTileEntities()).filter(_containerChecker).collect(Collectors.toList());
								totalEntitiesLoaded += containers.size();
								if(containers.size() > 0)
								{
									//logger.info("There are " + containers.size() + " containers to check.");

									for (BlockState container : containers)
									{
										//logger.info("Looking at a " + container.getType());
										if(container instanceof InventoryHolder)
											DecayInventory(((InventoryHolder) container).getInventory());
										else
											logger.warning("Can't check inventory of Blockstate: " + container.getType());
									}
								}

								// Vehicles, however, are Entities and can be InventoryHolders, but won't have a BlockState,
								// so I'm getting them this way.
								Object[] entities = Arrays.stream(chunk.getEntities()).filter(_storageEntityChecker).toArray();
								totalEntitiesLoaded += entities.length;
								if(entities.length > 0)
								{
									//logger.info("There are " + entities.length + " unchecked entities to check.");

									for(Object entity: entities)
									{
										if(entity instanceof InventoryHolder)
											DecayInventory(((InventoryHolder) entity).getInventory());
									}
								}
							}
						}
						//logger.info("A total of " + totalEntitiesLoaded + " containers have been checked in " + loadedChunks.length + " loaded chunks, and food has been decayed.");
					}
				},
				(_decayCheckInterval * 20L), // In server ticks. 1 second = 20 server ticks
				(_decayCheckInterval * 20L) // In server ticks. 1 second = 20 server ticks
		).getTaskId();
	}

	public static DecayHandler GetInstance(JavaPlugin plugin)
	{
		if(_decayHandler == null)
			_decayHandler = new DecayHandler(plugin);

		return _decayHandler;
	}

	public static void UpdateConfig(FileConfiguration config)
	{
		_decayingFoods.clear();
		for(String decayingFoodName: config.getStringList("DecayingFoods"))
		{
			// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
			_decayingFoods.add(Material.getMaterial(decayingFoodName.toUpperCase().replace(' ', '_')));
		}

		_decayStoppers.clear();
		for(String decayStopperName: config.getStringList("DecayStoppers"))
		{
			// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
			_decayStoppers.add(Material.getMaterial(decayStopperName.toUpperCase().replace(' ', '_')));
		}

		_rateOfDecay = config.getInt("RateOfDecay");
		_decayCheckInterval = config.getInt("DecayCheckInterval");
		if(_decayCheckerTaskId != -1) Bukkit.getScheduler().cancelTask(_decayCheckerTaskId);
		StartRepeatingDecayCheck(_plugin);
	}

	private static void DecayInventory(Inventory inventory)
	{
		for(Material _decayStopper : _decayStoppers)
		{
			if (_decayStopper != null && inventory.contains(_decayStopper))
			{
				//logger.info("Food is put on ice, so skipping decay check...");
				return;
			}
		}

		for (Material _perishable : _decayingFoods)
		{
			if (_perishable != null && inventory.contains(_perishable))
			{
				//logger.info(String.format("Container has %s in their inventory!", _perishableName));
				HashMap<Integer, ? extends ItemStack> similarItemStacks = inventory.all(_perishable);

				// Using the keyset instead of the values, since I need to replace the item at its slotindex
				for (int invItemStackKey : similarItemStacks.keySet())
				{
					ItemStack invItemStack = similarItemStacks.get(invItemStackKey);
					PersistentDataContainer invPdc = invItemStack.getItemMeta().getPersistentDataContainer();

					if (invPdc.has(_customDataKeys.expirationDate, PersistentDataType.STRING)
							&& CheckIfTimestampExpired(invPdc.get(_customDataKeys.expirationDate, PersistentDataType.STRING)))
					{
						//logger.info("Rotting food found, converting...");
						_rottenFood.setAmount(invItemStack.getAmount());
						inventory.setItem(invItemStackKey, _rottenFood);
						//logger.info("Food is now rotten!");
					}
				}
			}
		}
	}

	// Listen to playerRenameItem event and cancel if it's a decay item?
	// To make it compostable, probs need to do something similar

	private static boolean CheckIfTimestampExpired(String _foodDecayTimestamp)
	{
		Calendar calendar = Calendar.getInstance();
		String[] foodDecayTimestamp = _foodDecayTimestamp.split("-");

		Date nowDate = calendar.getTime();
		logger.info("Now: " + _timeFormat.format(calendar.getTime()));

		calendar.set(Integer.parseInt(foodDecayTimestamp[0]),		//Year
				Integer.parseInt(foodDecayTimestamp[1]) - 1,	//Month, only one that starts at 0, so need to do -1 for the right month
				Integer.parseInt(foodDecayTimestamp[2]),			//Day
				Integer.parseInt(foodDecayTimestamp[3]),			//Hour
				Integer.parseInt(foodDecayTimestamp[4]),			//Minute
				Integer.parseInt(foodDecayTimestamp[5]));			//Second
		logger.info("ExpirationTime: " + _timeFormat.format(calendar.getTime()));

		return calendar.getTime().before(nowDate);
	}

	public void AddDecayTimeIfDecayingFood(ItemStack droppedItemStack)
	{
		if (_decayingFoods.contains(droppedItemStack.getType()))
		{
			ItemMeta droppedItemMeta = droppedItemStack.getItemMeta();
			PersistentDataContainer droppedPdc = droppedItemMeta.getPersistentDataContainer();
			//logger.info("Got pdc of dropped item: " + droppedPdc.hashCode() + ". It has " + droppedPdc.getKeys().size() + " keys.");
			if (!droppedPdc.has(_customDataKeys.expirationDate, PersistentDataType.STRING))
			{
				//logger.info("There is no expiration date stored yet...");
				Calendar now = Calendar.getInstance();
				now.add(Calendar.SECOND, _rateOfDecay);
				droppedPdc.set(_customDataKeys.expirationDate, PersistentDataType.STRING, _timeFormat.format(now.getTime()));
				droppedItemStack.setItemMeta(droppedItemMeta);
				//logger.info("Expiration date of " + _config.getInt("RateOfDecay") + " seconds stored!");
			}
		}
	}
}

class ContainerChecker implements Predicate<BlockState>
{
	// Enderchest is tied to a player, so it would have to be checked through them!
	private final List<Material> containers = List.of(Material.BARREL, Material.BLAST_FURNACE, Material.CHEST, Material.DISPENSER,
			Material.DROPPER, Material.FURNACE, Material.HOPPER, Material.SHULKER_BOX, Material.SMOKER,
			Material.TRAPPED_CHEST,	Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
			Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
			Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
			Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
			Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX);

	@Override
	public boolean test(BlockState blockState)
	{
		return containers.contains(blockState.getBlockData().getMaterial());
	}
}

class StorageEntityChecker implements Predicate<Entity>
{
	private final List<EntityType> entities = List.of(EntityType.MINECART_HOPPER, EntityType.MINECART_CHEST,
			EntityType.MULE, EntityType.DONKEY, EntityType.LLAMA, EntityType.TRADER_LLAMA);

	@Override
	public boolean test(Entity entity)
	{
		return entities.contains(entity.getType());
	}
}