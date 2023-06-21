package com.redchestraven.food.fooddecay.consts;

import org.bukkit.event.inventory.InventoryAction;
import java.util.List;

public final class InvActionGroups
{
	public static final List<InventoryAction> tradeActions = List.of(InventoryAction.HOTBAR_SWAP, InventoryAction.PICKUP_ALL,
			InventoryAction.PICKUP_HALF, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY);

	public static final List<InventoryAction> multiTradeActions = List.of(InventoryAction.MOVE_TO_OTHER_INVENTORY);

	public static final List<InventoryAction> pickupOtherInvActions = List.of(InventoryAction.HOTBAR_SWAP, InventoryAction.PICKUP_ALL,
			InventoryAction.PICKUP_HALF, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY,
			InventoryAction.HOTBAR_MOVE_AND_READD, InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT);

	public static final List<InventoryAction> pausePutInInvActions = List.of(InventoryAction.HOTBAR_SWAP,
			InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
			InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.HOTBAR_MOVE_AND_READD);
}
