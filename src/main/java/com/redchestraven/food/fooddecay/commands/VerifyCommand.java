package com.redchestraven.food.fooddecay.commands;

import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class VerifyCommand implements CommandExecutor
{
	private static Logger logger = Logger.getLogger("Server");

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		if(args.length > 0)
		{
			logger.info("Command not recognised.");
			return false;
		}

		return VerifyConfig(sender);
	}

	public static boolean VerifyConfig(CommandSender sender)
	{
		boolean sentByPlayer = sender instanceof Player;
		logger.info("Verifying config file...");
		if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Verifying config file..."); }

		FileConfiguration _config = null;
		try{
			FileReader configReader = new FileReader("plugins\\FoodDecay\\config.yml");
			_config = YamlConfiguration.loadConfiguration(configReader);
		}
		catch(Exception e)
		{
			logger.severe("Config not found or can't be read for some reason. Please ensure the config" +
					"exists and isn't opened in another program, then try again.");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "Config not found or can't be read for some reason." +
					"Please ensure the config exists and isn't opened in another program, then try again."); }
		}

		if(_config == null)
		{
			return false;
		}

		/*===============================*
		 | Verifying decaying foods list |
		 *===============================*/
		logger.info("Verifying decaying food groups...");
		ConfigurationSection _decayingFoodGroups = _config.getConfigurationSection(ConfigSettingNames.decayingFoodGroups);

		if (_decayingFoodGroups == null || _decayingFoodGroups.getKeys(false).isEmpty())
		{
			logger.severe("The food groups list is empty, missing or isn't correctly written. Disabling FoodDecay...");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED
					+ "The food groups list is empty, missing or isn't correctly written. Disabling FoodDecay..."); }
			return false;
		}
		else
		{
			//Check if each group has a valid rate of decay, and all decaying food is present in exactly one (1) group
			HashSet<String> _decayingFoodNames = new HashSet<>();
			for (String _decayingFoodGroupName: _decayingFoodGroups.getKeys(false))
			{
				//logger.info("Checking food group " + _decayingFoodGroupName);

				//Check if the rate of decay of this group is valid
				String _rateOfDecayString = _config.getString(_decayingFoodGroupName + "." + ConfigSettingNames.rateOfDecay);
				try
				{
					if (_rateOfDecayString != null)
					{
						int _rateOfDecay = Integer.parseInt(_rateOfDecayString);
						if (_rateOfDecay <= 0)
						{
							logger.severe("The rate of decay for food group " + _decayingFoodGroupName
									+ "has an invalid value. It should be higher than 0, and recommended to be 120 for most servers. Disabling FoodDecay...");
							if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The rate of decay for food group " + _decayingFoodGroupName
									+ "has an invalid value. It should be higher than 0, and recommended to be 120 for most servers. Disabling FoodDecay..."); }
							return false;
						}
					}
				}
				catch (NumberFormatException nfe)
				{
					logger.severe("The rate of decay is text, instead of a number. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The rate of decay is text, instead of a number. Disabling FoodDecay..."); }
					return false;
				}

				//Check for food in this group not already existing in another group
				List<String> decayingFoods = _decayingFoodGroups.getStringList(_decayingFoodGroupName + "." + ConfigSettingNames.decayingFoods);
				if (decayingFoods.isEmpty())
				{
					logger.severe("The food group list " + _decayingFoodGroupName
							+ " is empty, missing or isn't correctly written. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The food group list " + _decayingFoodGroupName
							+ " is empty, missing or isn't correctly written. Disabling FoodDecay..."); }
					return false;
				}
				else
				{
					for (String decayingFoodName: decayingFoods)
					{
						decayingFoodName = decayingFoodName.toUpperCase().replace(' ', '_');
						//logger.info("Checking food " + decayingFoodName);
						if (!_decayingFoodNames.add(decayingFoodName))
						{
							logger.severe("The food " + decayingFoodName
									+ " is present in multiple groups. Disabling FoodDecay...");
							if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The food " + decayingFoodName
									+ " is present in multiple groups. Disabling FoodDecay..."); }
							return false;
						}

						Material decayingFoodMaterial = Material.getMaterial(decayingFoodName);
						if (decayingFoodMaterial == null)
						{
							logger.severe("The food " + decayingFoodName + " isn't a Material. Disabling FoodDecay...");
							if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The food "
									+ decayingFoodName + " isn't a Material. Disabling FoodDecay..."); }
							return false;
						}
						else if (!decayingFoodMaterial.isItem())
						{
							logger.severe("The food " + decayingFoodName + " isn't an Item. Disabling FoodDecay...");
							if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The food "
									+ decayingFoodName + " isn't an Item. Disabling FoodDecay..."); }
							return false;
						}
						/* Disabling check for just edible foods, as there is stuff like wheat we might want to decay too...
						else if(!decayingFoodMaterial.isEdible())
						{
							logger.severe("The food " + decayingFoodName + " isn't edible. Disabling FoodDecay...");
							return false;
						}*/
					}
				}
			}
		}

		/*===============================*
		 | Verifying decay stoppers list |
		 *===============================*/
		logger.info("Decaying foods verified, verifying decay stoppers...");
		List<String> decayStoppers = _config.getStringList(ConfigSettingNames.decayStoppers);
		if (decayStoppers.isEmpty())
		{
			logger.severe("The decay stoppers list is empty, missing or isn't correctly written. Disabling FoodDecay...");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED
					+ "The decay stoppers list is empty, missing or isn't correctly written. Disabling FoodDecay..."); }
			return false;
		}
		else
		{
			for (String decayStopperName: decayStoppers)
			{
				//logger.info("Checking decaystopper " + decayStopperName);
				if (!decayStopperName.toLowerCase().contains("ice"))
				{
					logger.severe("The decaystopper " + decayStopperName + " isn't a variant of ice. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The decaystopper "
							+ decayStopperName + " isn't a variant of ice. Disabling FoodDecay..."); }
					return false;
				}
				Material perishable = Material.getMaterial(decayStopperName.toUpperCase().replace(' ', '_'));
				if (perishable == null)
				{
					logger.severe("The decaystopper " + decayStopperName + " isn't a Material. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The decaystopper "
							+ decayStopperName + " isn't a Material. Disabling FoodDecay..."); }
					return false;
				}
				else if (!perishable.isBlock())
				{
					logger.severe("The decaystopper " + decayStopperName + " isn't a block. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The decaystopper "
							+ decayStopperName + " isn't a block. Disabling FoodDecay..."); }
					return false;
				}
			}
		}

		/*=================================*
		 | Verifying time-related settings |
		 *=================================*/
		logger.info("Decay stoppers verified, verifying time-related settings...");
		String _decayIntervalString = _config.getString(ConfigSettingNames.decayCheckInterval);
		try
		{
			if (_decayIntervalString != null)
			{
				int _decayInterval = Integer.parseInt(_decayIntervalString);
				if (_decayInterval <= 0)
				{
					logger.severe("The interval has an invalid value. It should be higher than 0, and recommended to be 60 for most servers. Disabling FoodDecay...");
					if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The interval has an invalid value."
							+ "It should be higher than 0, and recommended to be 60 for most servers. Disabling FoodDecay..."); }
					return false;
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			logger.severe("The interval is text, instead of a number. Disabling FoodDecay...");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "The interval is text, instead of a number. Disabling FoodDecay..."); }
			return false;
		}

		/*========================*
		 | Verifying worlds exist |
		 *========================*/
		logger.info("Time-related settings verified, verifying worlds...");
		List<String> _worldNamesFromConfig = _config.getStringList(ConfigSettingNames.worlds);
		List<String> _existingWorldNames = Bukkit.getWorlds().stream().map(WorldInfo::getName).collect(Collectors.toList());
		for (String worldNameFromConfig: _worldNamesFromConfig)
		{
			if (!_existingWorldNames.contains(worldNameFromConfig))
			{
				logger.severe("This world does not exist on your server! Disabling FoodDecay...");
				if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "This world does not exist on your server! Disabling FoodDecay..."); }
				return false;
			}
		}

		if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Config has been verified!"); }
		return true;
	}
}