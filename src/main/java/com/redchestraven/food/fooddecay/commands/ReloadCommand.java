package com.redchestraven.food.fooddecay.commands;

import com.redchestraven.food.fooddecay.DecayHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.listeners.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class ReloadCommand implements CommandExecutor
{
	private final Logger logger;
	private final JavaPlugin _plugin;

	public ReloadCommand(JavaPlugin plugin)
	{
		logger = Logger.getLogger("FoodDecay");
		_plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		logger.info("Config being reloaded...");
		_plugin.reloadConfig();
		FileConfiguration config = _plugin.getConfig();
		logger.info("Config reloaded! Verifying...");
		FoodDecay.SetEnabled(new VerifyCommand(_plugin).VerifyConfig());
		if (FoodDecay._enabled)
		{
			logger.info("Config verified, applying changes!");
			DecayHandler.UpdateConfig(config);
			GenerateFoodAsLootListener.UpdateConfig(config);
			ObtainFoodListener.UpdateConfig(config);
			TradeForFoodListener.UpdateConfig(config);
			logger.info("Changes applied, FoodDecay is fully ready to go!");
		}
		else
			logger.severe("Config isn't correct, please fix and reload again to allow for decaying food.");

		return true;
	}
}