package com.redchestraven.food.fooddecay.consts;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.List;

public final class ContainerGroups
{
	/*==================*
	 | Block containers |
	 *==================*/
	public static final List<Material> containers = List.of(Material.BARREL, Material.BLAST_FURNACE, Material.CHEST, Material.DISPENSER,
			Material.DROPPER, Material.FURNACE, Material.HOPPER, Material.SHULKER_BOX, Material.SMOKER,
			Material.TRAPPED_CHEST,	Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
			Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
			Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
			Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
			Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX);

	/*===================*
	 | Entity containers |
	 *===================*/
	public static final List<EntityType> entities = List.of(EntityType.MINECART_HOPPER, EntityType.MINECART_CHEST,
			EntityType.MULE, EntityType.DONKEY, EntityType.LLAMA, EntityType.TRADER_LLAMA);
}
