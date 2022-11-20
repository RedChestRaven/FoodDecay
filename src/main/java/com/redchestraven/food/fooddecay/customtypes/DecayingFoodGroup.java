package com.redchestraven.food.fooddecay.customtypes;

import org.bukkit.Material;

import java.util.ArrayList;

public class DecayingFoodGroup
{
	private String _name;
	private int _rateOfDecay;
	private ArrayList<Material> _decayingFoods;

	public DecayingFoodGroup(String name, int rateOfDecay, ArrayList<Material> decayingFoods)
	{
		_name = name;
		_rateOfDecay = rateOfDecay;
		_decayingFoods = decayingFoods;
	}

	public String GetName()
	{
		return _name;
	}

	public int GetRateOfDecay()
	{
		return _rateOfDecay;
	}

	public ArrayList<Material> GetDecayingFoods()
	{
		return _decayingFoods;
	}
}
