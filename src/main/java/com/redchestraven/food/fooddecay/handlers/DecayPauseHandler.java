package com.redchestraven.food.fooddecay.handlers;

import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import com.redchestraven.food.fooddecay.consts.CustomDataKeys;
import com.redchestraven.food.fooddecay.consts.Formatters;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public final class DecayPauseHandler implements Listener
{
	private static DecayPauseHandler _decayPauseHandler = null;
	private static final HashSet<Material> _decayStoppers = new HashSet<>();
	private static final Logger logger = Logger.getLogger("FoodDecay");

	private DecayPauseHandler(JavaPlugin plugin)
	{
		UpdateConfig(plugin.getConfig());
	}

	public static DecayPauseHandler GetInstance(JavaPlugin plugin)
	{
		if (_decayPauseHandler == null)
			_decayPauseHandler = new DecayPauseHandler(plugin);

		return _decayPauseHandler;
	}

	public static void TogglePausedTime(Inventory inv, ItemStack food)
	{
		ItemMeta foodMeta = food.getItemMeta();
		PersistentDataContainer foodPdc = foodMeta.getPersistentDataContainer();

		boolean decayStopperInInv = false;
		for (Material decayStopper : _decayStoppers)
		{
			if (inv.contains(decayStopper))
			{
				decayStopperInInv = true;
				break;
			}
		}

		if (decayStopperInInv)
		{
			if (!foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
			{
				String[] decayTimestampStringified = foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING).split("-");
				logger.warning("Setting paused time left before decay...");
				logger.info("Now: " + LocalDateTime.now().format(Formatters.internalTimeFormat));
				logger.info("Time of decay: " + foodPdc.get(CustomDataKeys.expirationDate, PersistentDataType.STRING));
				LocalDateTime decayTimestamp = LocalDateTime.of(Integer.parseInt(decayTimestampStringified[0]),
						Integer.parseInt(decayTimestampStringified[1]),
						Integer.parseInt(decayTimestampStringified[2]),
						Integer.parseInt(decayTimestampStringified[3]),
						Integer.parseInt(decayTimestampStringified[4]),
						Integer.parseInt(decayTimestampStringified[5]));
				LocalDateTime now = LocalDateTime.now();
				LocalDateTime pausedTimeLeft = decayTimestamp.minusYears(now.getYear())
															.minusMonths(now.getMonthValue())
															.minusDays(now.getDayOfMonth())
															.minusHours(now.getHour())
															.minusMinutes(now.getMinute())
															.minusSeconds(now.getSecond());

				logger.info("Paused time left before decay: " + String.join("-", pausedTimeLeft.format(Formatters.internalTimeFormat)));
				foodPdc.set(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING, String.join("-", pausedTimeLeft.format(Formatters.internalTimeFormat)));

				List<String> lore = foodMeta.getLore();
				int loreLineToAdjust = -1;
				for (int i = 0; i < lore.stream().count(); i++)
				{
					if (lore.get(i).contains("Will decay at:"))
					{
						loreLineToAdjust = i;
						break;
					}
				}
				if (loreLineToAdjust >= 0)
					lore.set(loreLineToAdjust, ChatColor.DARK_GREEN + "Decay paused.");
				foodMeta.setLore(lore);
				food.setItemMeta(foodMeta);
			}
		}
		else
		{
			if (foodPdc.has(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING))
			{
				String[] pausedTimeLeft = foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING).split("-");
				logger.warning("Setting new decay timestamp...");
				logger.info("Now: " + LocalDateTime.now().format(Formatters.internalTimeFormat));
				logger.info("Paused time left: " + foodPdc.get(CustomDataKeys.pausedTimeLeft, PersistentDataType.STRING));
				LocalDateTime newDecayTimestamp = LocalDateTime.now().plusYears(Integer.parseInt(pausedTimeLeft[0]))
																	.plusMonths(Integer.parseInt(pausedTimeLeft[1]))
																	.plusDays(Integer.parseInt(pausedTimeLeft[2]))
																	.plusHours(Integer.parseInt(pausedTimeLeft[3]))
																	.plusMinutes(Integer.parseInt(pausedTimeLeft[4]))
																	.plusSeconds(Integer.parseInt(pausedTimeLeft[5]));

				logger.info("New decay timestamp: " + String.join("-", newDecayTimestamp.format(Formatters.internalTimeFormat)));
				foodPdc.set(CustomDataKeys.expirationDate, PersistentDataType.STRING, String.join("-", newDecayTimestamp.format(Formatters.internalTimeFormat)));
				foodPdc.remove(CustomDataKeys.pausedTimeLeft);

				List<String> lore = foodMeta.getLore();
				int loreLineToAdjust = -1;
				for (int i = 0; i < lore.stream().count(); i++)
				{
					if (lore.get(i).contains("Decay paused."))
					{
						loreLineToAdjust = i;
						break;
					}
				}
				if (loreLineToAdjust > -1)
					lore.set(loreLineToAdjust, ChatColor.DARK_GREEN + newDecayTimestamp.format(Formatters.loreTimeFormat));
				foodMeta.setLore(lore);
				food.setItemMeta(foodMeta);
			}
		}
	}

	public static void UpdateConfig(FileConfiguration config)
	{
		logger.info("Updating DecayPauseHandler decaystoppers: " + config.getStringList(ConfigSettingNames.decayStoppers));
		_decayStoppers.clear();
		for (String decayStopperName: config.getStringList(ConfigSettingNames.decayStoppers))
		{
			// Making sure Material is always correctly formatted, while allowing for spaces being used to make the config more readable
			_decayStoppers.add(Material.getMaterial(decayStopperName.toUpperCase().replace(' ', '_')));
		}
	}
}