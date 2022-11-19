package com.redchestraven.food.fooddecay.consts;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class CustomDataKeys
{
	public CustomDataKeys(){ }

	// Just to make it easier to add new NamespacedKeys
	private static final Plugin plugin = Bukkit.getPluginManager().getPlugin("FoodDecay");

	public final NamespacedKey expirationDate = new NamespacedKey(plugin, "expirationtime");
}