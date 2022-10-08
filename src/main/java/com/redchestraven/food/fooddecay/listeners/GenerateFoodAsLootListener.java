package com.redchestraven.food.fooddecay.listeners;

import com.redchestraven.food.fooddecay.DecayHandler;
import com.redchestraven.food.fooddecay.FoodDecay;
import com.redchestraven.food.fooddecay.consts.EventNames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GenerateFoodAsLootListener implements Listener
{
	private static final Logger logger = Logger.getLogger("FoodDecay");
	private static GenerateFoodAsLootListener _generateFoodAsLootListener = null;
	private static DecayHandler _decayHandler;
	private static final Map<String, Boolean> _enabledEvents = new HashMap<>();

	private GenerateFoodAsLootListener(JavaPlugin plugin)
	{
		_decayHandler = DecayHandler.GetInstance(plugin);
		UpdateConfig(plugin.getConfig());
	}

	public static GenerateFoodAsLootListener GetInstance(JavaPlugin plugin)
	{
		if(_generateFoodAsLootListener == null)
			_generateFoodAsLootListener = new GenerateFoodAsLootListener(plugin);

		return _generateFoodAsLootListener;
	}

	public static void UpdateConfig(FileConfiguration config)
	{
		//logger.info("Events before reload: " + _enabledEvents.get(EventNames.onDropFromBlockBreak)
		//		+ " | "	+ _enabledEvents.get(EventNames.onContainerLootGenerated)
		//		+ " | "	+ _enabledEvents.get(EventNames.onDropFromEntityDeath)
		//		+ " | "	+ _enabledEvents.get(EventNames.onFishedUp));

		_enabledEvents.clear();
		_enabledEvents.put(EventNames.onDropFromBlockBreak, config.getBoolean(EventNames.onDropFromBlockBreak));
		_enabledEvents.put(EventNames.onContainerLootGenerated, config.getBoolean(EventNames.onContainerLootGenerated));
		_enabledEvents.put(EventNames.onDropFromEntityDeath, config.getBoolean(EventNames.onDropFromEntityDeath));
		_enabledEvents.put(EventNames.onFishedUp, config.getBoolean(EventNames.onFishedUp));

		//logger.info("Events after reload: " + _enabledEvents.get(EventNames.onDropFromBlockBreak)
		//		+ " | "	+ _enabledEvents.get(EventNames.onContainerLootGenerated)
		//		+ " | "	+ _enabledEvents.get(EventNames.onDropFromEntityDeath)
		//		+ " | "	+ _enabledEvents.get(EventNames.onFishedUp));
	}

	@EventHandler
	public void OnFoodHarvested(BlockDropItemEvent bdie)
	{
		if(FoodDecay._enabled && _enabledEvents.get("OnDropFromBlockBreak"))
		{
			for (Item droppedItem : bdie.getItems())
			{
				ItemStack droppedItemStack = droppedItem.getItemStack();

				_decayHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
			}
		}
	}

	@EventHandler
	public void OnFoodGeneratedAsLoot(LootGenerateEvent lge)
	{
		if(FoodDecay._enabled && _enabledEvents.get("OnContainerLootGenerated"))
		{
			for (ItemStack droppedItemStack : lge.getLoot())
			{
				_decayHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
			}
		}
	}

	@EventHandler
	public void OnFoodGeneratedAsMobDrop(EntityDeathEvent ede)
	{
		if(FoodDecay._enabled && _enabledEvents.get("OnDropFromEntityDeath"))
		{
			for (ItemStack droppedItemStack : ede.getDrops())
			{
				_decayHandler.AddDecayTimeIfDecayingFood(droppedItemStack);
			}
		}
	}

	@EventHandler
	public void OnFoodFishedUp(PlayerFishEvent pfe)
	{
		//logger.info("PlayerFishEvent state: " + pfe.getState() + ". Needed state: " + PlayerFishEvent.State.CAUGHT_FISH);

		if(FoodDecay._enabled && _enabledEvents.get("OnFishedUp") && pfe.getCaught() != null
				&& pfe.getState().equals(PlayerFishEvent.State.CAUGHT_FISH))
		{
				_decayHandler.AddDecayTimeIfDecayingFood(((Item) pfe.getCaught()).getItemStack());
		}
	}
}
