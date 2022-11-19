package com.redchestraven.food.fooddecay.customtypes;

import org.bukkit.Material;

import java.util.ArrayList;

public class DecayingFoodGroup
{
	private String _name;
	private ArrayList<Material> _decayingFoods;

	public DecayingFoodGroup(String name, ArrayList<Material> decayingFoods)
	{
		_name = name;
		_decayingFoods = decayingFoods;
	}

	public String GetName()
	{
		return _name;
	}

	public ArrayList<Material> GetDecayingFoods()
	{
		return _decayingFoods;
	}
}
