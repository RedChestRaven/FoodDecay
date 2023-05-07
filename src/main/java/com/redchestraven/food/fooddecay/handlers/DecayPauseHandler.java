package com.redchestraven.food.fooddecay.handlers;

import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public final class DecayPauseHandler implements Listener
{
	private static DecayPauseHandler _decayPauseHandler = null;
	private static final HashSet<Material> _decayStoppers = new HashSet<>();
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static final DateFormat _internalTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	private DecayPauseHandler(JavaPlugin plugin)
	{
		UpdateConfig(plugin.getConfig());
	}

	public static DecayPauseHandler GetInstance(JavaPlugin plugin)
	{
		if(_decayPauseHandler == null)
			_decayPauseHandler = new DecayPauseHandler(plugin);

		return _decayPauseHandler;
	}

	public static void TogglePausedTime(Inventory inv, ItemStack food)
	{
		ItemMeta foodMeta = food.getItemMeta();
		PersistentDataContainer foodPdc = foodMeta.getPersistentDataContainer();


		for(Material decayStopper: _decayStoppers)
		{
			if(inv.contains(decayStopper))
			{
				if(!foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
				{
					String[] decayTimestampStringified = foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING).split("-");
					logger.warning("Setting paused time left before decay...");
					logger.info("Now: " + _internalTimeFormat.format(Calendar.getInstance().getTime()));
					logger.info("Time of decay: " + foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING));
					Calendar pausedTimeLeft = Calendar.getInstance();
					pausedTimeLeft.set(
							Integer.parseInt(decayTimestampStringified[0]) - pausedTimeLeft.get(Calendar.YEAR),
							Integer.parseInt(decayTimestampStringified[1]) - pausedTimeLeft.get(Calendar.MONTH),
							Integer.parseInt(decayTimestampStringified[2]) - pausedTimeLeft.get(Calendar.DATE),
							Integer.parseInt(decayTimestampStringified[3]) - pausedTimeLeft.get(Calendar.HOUR),
							Integer.parseInt(decayTimestampStringified[4]) - pausedTimeLeft.get(Calendar.MINUTE),
							Integer.parseInt(decayTimestampStringified[5]) - pausedTimeLeft.get(Calendar.SECOND)
					);

					logger.info("Paused time left before decay: " + String.join("-", _internalTimeFormat.format(pausedTimeLeft.getTime())));
					foodPdc.set(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING, String.join("-", _internalTimeFormat.format(pausedTimeLeft.getTime())));

					List<String> lore = foodMeta.getLore();
					int loreLineToRemove = -1;
					for(int i = 0; i < lore.stream().count(); i++)
					{
						if(lore.get(i).contains("Will decay at:"))
						{
							loreLineToRemove = i;
							break;
						}
					}
					if(loreLineToRemove > -1)
						lore.set(loreLineToRemove, ChatColor.DARK_GREEN + "Decay paused.");
					foodMeta.setLore(lore);
					food.setItemMeta(foodMeta);
				}
			}
			else
			{
				if(foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
				{
					String[] pausedTimeLeft = foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING).split("-");
					logger.warning("Setting new decay timestamp...");
					logger.info("Now: " + _internalTimeFormat.format(Calendar.getInstance().getTime()));
					logger.info("Paused time left: " + foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));
					Calendar newDecayTimestamp = Calendar.getInstance();
					newDecayTimestamp.add(Calendar.YEAR, Integer.parseInt(pausedTimeLeft[0]));
					newDecayTimestamp.add(Calendar.MONTH, Integer.parseInt(pausedTimeLeft[1]) - 1);
					newDecayTimestamp.add(Calendar.DATE, Integer.parseInt(pausedTimeLeft[2]));
					newDecayTimestamp.add(Calendar.HOUR, Integer.parseInt(pausedTimeLeft[3]));
					newDecayTimestamp.add(Calendar.MINUTE, Integer.parseInt(pausedTimeLeft[4]));
					newDecayTimestamp.add(Calendar.SECOND, Integer.parseInt(pausedTimeLeft[5]));

					logger.info("New decay timestamp: " + String.join("-", _internalTimeFormat.format(newDecayTimestamp.getTime())));
					foodPdc.set(CustomDataKeys.expirationDate, PersistentDataType.STRING, String.join("-", _internalTimeFormat.format(newDecayTimestamp.getTime())));
					foodPdc.remove(CustomDataKeys.pausedTimeLeft);

					List<String> lore = foodMeta.getLore();
					int loreLineToRemove = -1;
					for(int i = 0; i < lore.stream().count(); i++)
					{
						if(lore.get(i).contains("Decay paused."))
						{
							loreLineToRemove = i;
							break;
						}
					}
					if(loreLineToRemove > -1)
						lore.set(loreLineToRemove, ChatColor.DARK_GREEN + "Will decay at:");
					foodMeta.setLore(lore);
					food.setItemMeta(foodMeta);
				}
			}
		}
	}

	public static void UpdateConfig(FileConfiguration config)
	{
		logger.info("Updating DecayPauseHandler decaystoppers: " + config.getStringList(ConfigSettingNames.decayStoppers));
		_decayStoppers.clear();
		for(String decayStopperName: config.getStringList(ConfigSettingNames.decayStoppers))
		{
			// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
			_decayStoppers.add(Material.getMaterial(decayStopperName.toUpperCase().replace(' ', '_')));
		}
	}
}
