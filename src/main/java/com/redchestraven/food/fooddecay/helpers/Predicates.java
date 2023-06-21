package com.redchestraven.food.fooddecay.helpers;

import com.redchestraven.food.fooddecay.consts.ContainerGroups;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

public final class Predicates
{
	private static Predicates _predicates = null;
	private static ContainerChecker _containerChecker = null;
	private static StorageEntityChecker _storageEntityChecker = null;

	private Predicates()
	{
		_containerChecker = new ContainerChecker();
		_storageEntityChecker = new StorageEntityChecker();
	}

	public static Predicates GetInstance()
	{
		if (_predicates == null)
			_predicates = new Predicates();

		return _predicates;
	}

	public ContainerChecker GetContainerChecker()
	{
		return _containerChecker;
	}

	public StorageEntityChecker GetStorageEntityChecker()
	{
		return _storageEntityChecker;
	}
}

class ContainerChecker implements Predicate<BlockState>
{
	@Override
	public boolean test(BlockState blockState)
	{
		return ContainerGroups.containers.contains(blockState.getBlockData().getMaterial());
	}
}

class StorageEntityChecker implements Predicate<Entity>
{
	@Override
	public boolean test(Entity entity)
	{
		return ContainerGroups.entities.contains(entity.getType());
	}
}