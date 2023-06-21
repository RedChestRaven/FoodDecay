package com.redchestraven.food.fooddecay.consts;

import java.time.format.DateTimeFormatter;

public final class Formatters
{
	public static final DateTimeFormatter internalTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
	public static final String[] internalPausedTimeFormat = {"%04d","%02d","%02d","%02d","%02d","%02d"};
	public static final DateTimeFormatter loreTimeFormat = DateTimeFormatter.ofPattern("dd MMM HH:mm");
}