package com.redchestraven.food.fooddecay.commands;

import com.redchestraven.food.fooddecay.consts.CommandNames;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class BaseCommand implements CommandExecutor
{
	private final Logger logger;
	private JavaPlugin _plugin;

	public BaseCommand(JavaPlugin plugin)
	{
		logger = Logger.getLogger("FoodDecay");
		_plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
	{
		logger.warning("Label = " + label);
		logger.warning("Command = " + command.getName());

		if(args.length == 1)
		{
			if(args[0].compareToIgnoreCase(CommandNames.reloadBase) == 0)
			{
				ReloadCommand.Reload(_plugin, sender);
				return true;
			}

			if(args[0].compareToIgnoreCase(CommandNames.verifyBase) == 0)
			{
				VerifyCommand.VerifyConfig(_plugin, sender);
				return true;
			}
		}

		logger.info("Command not recognised.");
		if(sender instanceof Player) { sender.sendMessage("Command not recognised."); }
		return false;
	}
}
