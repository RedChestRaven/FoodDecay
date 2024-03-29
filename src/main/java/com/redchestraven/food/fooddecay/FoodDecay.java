package com.redchestraven.food.fooddecay;

import com.redchestraven.food.fooddecay.commands.*;
import com.redchestraven.food.fooddecay.consts.CommandNames;
import com.redchestraven.food.fooddecay.consts.ConfigSettingNames;
import com.redchestraven.food.fooddecay.handlers.*;
import com.redchestraven.food.fooddecay.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public final class FoodDecay extends JavaPlugin
{
	private final Logger logger = this.getLogger();
	public static boolean _enabled = true;

	@Override
	public void onEnable()
	{
		// Plugin startup logic
		logger.info("Starting up FoodDecay...");
		//TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		// Create config if it doesn't exist already, since other parts require it
		logger.info("Checking if config already exists...");
		if (getDataFolder().exists() && (Set.of(getDataFolder().list()).contains("config.yml")))
		{
			logger.info("Config found, verifying...");
			String configVersion = getConfig().getString(ConfigSettingNames.version);
			String pluginVersion = getDescription().getVersion().split("\\.")[0];
			if (configVersion.equals(pluginVersion))
			{
				SetEnabled(VerifyCommand.VerifyConfig(this, getServer().getConsoleSender()));
				if (_enabled)
					logger.info("Config entirely verified, moving on!");
				else
					logger.severe("Config isn't entirely correct, please fix, reload and verify again to allow for decaying food.");
			}
			else
			{
				SetEnabled(false);
				logger.severe("Config is outdated. Your config's version is " + configVersion
						+ " and should be " + pluginVersion + ". FoodDecay will be disabled.");
				logger.severe("Please stop the server, back up your config, delete the original, "
						+ "apply your changes as applicable, and start the server again.");
				return;
			}
		}
		else
		{
			logger.info("No config found, creating default...");
			getConfig().options().copyDefaults();
			saveDefaultConfig();
			logger.info("Default config created!");
		}

		// Add commands
		logger.info("Setting up command listeners...");
		Objects.requireNonNull(getCommand(CommandNames.verifyShort)).setExecutor(new VerifyCommand(this));
		Objects.requireNonNull(getCommand(CommandNames.reloadShort)).setExecutor(new ReloadCommand(this));
		Objects.requireNonNull(getCommand(CommandNames.fdShort)).setExecutor(new BaseCommand(this));
		logger.info("Command listeners set up!");

		if (getConfig().getBoolean(ConfigSettingNames.useSimpleDecayCheck))
		{
			logger.warning("Starting up simple decay check...");
			if (SimpleDecayFoodHandler.StartInstance(this))
				logger.info("Simple decay check started!");
		}
		else
		{
			logger.warning("Setting up advanced decay check...");
			// Make Handlers and update their configs
			logger.info("Starting handlers and updating their configs...");
			DecayFoodHandler.GetInstance(this);
			DecayPauseHandler.GetInstance(this);
			logger.info("Handlers started with updated configs!");

			// Register events
			logger.info("Registering events...");
			Bukkit.getPluginManager().registerEvents(ObtainFoodListener.GetInstance(), this);
			Bukkit.getPluginManager().registerEvents(TradeForFoodListener.GetInstance(), this);
			Bukkit.getPluginManager().registerEvents(PauseDecayListener.GetInstance(), this);
			logger.info("Events registered!");
		}

		if (_enabled)
			logger.info("Plugin is fully loaded, enjoy decaying food!");
		else
			logger.severe("Plugin has core functions disabled.");
	}

	@Override
	public void onDisable()
	{
		// Plugin shutdown logic
		logger.info("Stopping FoodDecay...");
	}

	public static void SetEnabled(boolean enabled)
	{
		_enabled = enabled;
	}
}