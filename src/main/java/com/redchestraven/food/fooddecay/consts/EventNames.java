package com.redchestraven.food.fooddecay.consts;

public class EventNames{
	// ObtainFood events
	public static final String onPickupByPlayer = "OnPickupByPlayer";
	public static final String onPickupByHopper = "OnPickupByHopper";
	public static final String onNonPlayerMoveToOtherInventory = "OnNonPlayerMoveToOtherInventory";
	public static final String onPlayerPickupFromOtherInventory = "OnPlayerPickupFromOtherInventory";

	// GenerateFoodAsLoot events
	public static final String onDropFromBlockBreak = "OnDropFromBlockBreak";
	public static final String onContainerLootGenerated = "OnContainerLootGenerated";
	public static final String onDropFromEntityDeath = "OnDropFromEntityDeath";
	public static final String onFishedUp = "OnFishedUp";
}