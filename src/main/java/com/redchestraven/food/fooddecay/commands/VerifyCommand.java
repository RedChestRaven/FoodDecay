package com.redchestraven.food.fooddecay.commands;

import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.EventNames;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VerifyCommand implements CommandExecutor
{
	private final Logger logger;
	private FileConfiguration _config;
	private final JavaPlugin _plugin;

	public VerifyCommand(JavaPlugin plugin)
	{
		logger = Logger.getLogger("FoodDecay");
		_config = plugin.getConfig();
		_plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		logger.info("Verifying loaded config...");
		FoodDecay.SetEnabled(VerifyConfig());

		if(FoodDecay._enabled)
			logger.info("Config verified, enjoy FoodDecay!");
		else
			logger.severe("Config is incorrect, stopped FoodDecay...");

		return true;
	}

	public boolean VerifyConfig()
	{
		_config = _plugin.getConfig();

		/*=====================*
		 | Verifying food list |
		 *=====================*/
		List<String> _perishableFoods = _config.getStringList("DecayingFoods");
		if(_perishableFoods.isEmpty())
		{
			logger.severe("The perishable foods list is empty, missing or isn't correctly written. Disabling FoodDecay...");
			return false;
		}
		else
		{
			for(String _perishableName: _perishableFoods)
			{
				//logger.info("Checking food " + _perishableName);
				Material perishable = Material.getMaterial(_perishableName.toUpperCase().replace(' ', '_'));
				if(perishable == null)
				{
					logger.severe("The food " + _perishableName + " isn't a Material. Disabling FoodDecay...");
					return false;
				}
				else if(!perishable.isItem())
				{
					logger.severe("The food " + _perishableName + " isn't an Item. Disabling FoodDecay...");
					return false;
				}
				/* Disabling check for just edible foods, as there is stuff like wheat we might want to decay too...
				else if(!perishable.isEdible())
				{
					logger.severe("The food " + _perishableName + " isn't edible. Disabling FoodDecay...");
					return false;
				}*/
			}
		}

		/*====================*
		 | Verifying ice list |
		 *====================*/
		List<String> _decayStoppers = _config.getStringList("DecayStoppers");
		if(_decayStoppers.isEmpty())
		{
			logger.severe("The decay stoppers list is empty, missing or isn't correctly written. Disabling FoodDecay...");
			return false;
		}
		else
		{
			for(String _decayStopperName: _decayStoppers)
			{
				//logger.info("Checking decaystopper " + _decayStopperName);
				if(!_decayStopperName.toLowerCase().contains("ice"))
				{
					logger.severe("The decaystopper " + _decayStopperName + " isn't a variant of ice. Disabling FoodDecay...");
					return false;
				}
				Material perishable = Material.getMaterial(_decayStopperName.toUpperCase().replace(' ', '_'));
				if(perishable == null)
				{
					logger.severe("The decaystopper " + _decayStopperName + " isn't a Material. Disabling FoodDecay...");
					return false;
				}
				else if(!perishable.isBlock())
				{
					logger.severe("The decaystopper " + _decayStopperName + " isn't a block. Disabling FoodDecay...");
					return false;
				}
			}
		}

		/*=================================*
		 | Verifying time-related settings |
		 *=================================*/
		String _decayIntervalString = _config.getString("DecayCheckInterval");
		try
		{
			if(_decayIntervalString != null)
			{
				int _decayInterval = Integer.parseInt(_decayIntervalString);
				if (_decayInterval <= 0)
				{
					logger.severe("The interval has an invalid value. It should be higher than 0, and recommended to be 60 for most servers. Disabling FoodDecay...");
					return false;
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			logger.severe("The interval is text, instead of a number. Disabling FoodDecay...");
			return false;
		}

		String _rateOfDecayString = _config.getString("DecayCheckInterval");
		try
		{
			if (_rateOfDecayString != null)
			{
				int _rateOfDecay = Integer.parseInt(_rateOfDecayString);
				if (_rateOfDecay <= 0)
				{
					logger.severe("The rate of decay has an invalid value. It should be higher than 0, and recommended to be 120 for most servers. Disabling FoodDecay...");
					return false;
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			logger.severe("The rate of decay is text, instead of a number. Disabling FoodDecay...");
			return false;
		}

		/*========================*
		 | Verifying worlds exist |
		 *========================*/
		 List<String> _worldNamesFromConfig = _config.getStringList("Worlds");
		 List<String> _existingWorldNames = Bukkit.getWorlds().stream().map(WorldInfo::getName).collect(Collectors.toList());
		 for(String worldNameFromConfig: _worldNamesFromConfig)
		 {
			if(!_existingWorldNames.contains(worldNameFromConfig))
			{
				logger.severe("This world does not exist on your server! Disabling FoodDecay...");
				return false;
			}
		 }

		/*============================================*
		 | Verifying if at least one event is enabled |
		 *============================================*/
		boolean atLeastOneEventEnabled = _config.getBoolean(EventNames.onDropFromBlockBreak)
				|| _config.getBoolean(EventNames.onContainerLootGenerated)
				|| _config.getBoolean(EventNames.onDropFromEntityDeath)
				|| _config.getBoolean(EventNames.onFishedUp)
				|| _config.getBoolean(EventNames.onPickupByPlayer)
				|| _config.getBoolean(EventNames.onPickupByHopper)
				|| _config.getBoolean(EventNames.onNonPlayerMoveToOtherInventory)
				|| _config.getBoolean(EventNames.onPlayerPickupFromOtherInventory);

		if(!atLeastOneEventEnabled)
		{
			logger.severe("No events to make food decaying are enabled! Enable at least one of them to re-enable the plugin again. Disabling FoodDecay...");
			return false;
		}

		return true;
	}
}