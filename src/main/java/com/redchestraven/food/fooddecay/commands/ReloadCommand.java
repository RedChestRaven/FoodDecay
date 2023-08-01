package com.redchestraven.food.fooddecay.commands;

import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import com.redchestraven.food.fooddecay.handlers.DecayFoodHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.handlers.DecayPauseHandler;
import com.redchestraven.food.fooddecay.handlers.SimpleDecayFoodHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class ReloadCommand implements CommandExecutor
{
	private static Logger logger = Logger.getLogger("FoodDecay");
	private static JavaPlugin _plugin;

	public ReloadCommand(JavaPlugin plugin)
	{
		_plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		if(args.length > 0)
		{
			logger.info("Command not recognised.");
			return false;
		}

		return Reload(_plugin, sender);
	}

	public static boolean Reload(JavaPlugin plugin, CommandSender sender)
	{
		boolean sentByPlayer = sender instanceof Player;

		logger.info("Verifying config...");
		if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Verifying config..."); }
		FoodDecay.SetEnabled(VerifyCommand.VerifyConfig(sender));
		if (FoodDecay._enabled)
		{
			logger.info("Config verified, reloading...");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Config verified, reloading..."); }
			plugin.reloadConfig();
			FileConfiguration config = plugin.getConfig();

			logger.info("Config reloaded, applying changes!");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Config reloaded, applying changes!"); }
			if(config.getBoolean(ConfigSettingNames.useSimpleDecayCheck))
			{
				SimpleDecayFoodHandler.UpdateConfig(config);
			}
			else
			{
				DecayFoodHandler.UpdateConfig(config);
				DecayPauseHandler.UpdateConfig(config);
			}
			logger.info("Changes applied, FoodDecay is fully ready to go!");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_GREEN + "Changes applied, FoodDecay is fully ready to go!"); }
		}
		else
		{
			logger.severe("Config isn't correct, please fix and reload again to allow for decaying food.");
			if(sentByPlayer) { sender.sendMessage(ChatColor.DARK_RED + "Config isn't correct, please fix and reload again to allow for decaying food."); }
		}

		return true;
	}
}