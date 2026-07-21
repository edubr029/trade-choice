package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.config.TradeChoiceConfig;
import com.tradechoice.client.cycling.AutoSearchDriver;
import net.minecraft.client.Minecraft;

public final class TradeChoiceClient {

	private static TradeChoiceConfig config;
	private static TradeAlertManager alertManager;

	private TradeChoiceClient() {
	}

	public static void init() {
		config = new TradeChoiceConfig();
		config.load();
		alertManager = new TradeAlertManager(config);
		TradeChoiceMod.LOGGER.info(
				"[tradechoice] Client initialized, loaded {} choices (auto-search: 1 reply-in-flight, max 2000 cycles)",
				config.getChoices().size());
	}

	public static void tick(Minecraft mc) {
		AutoSearchDriver.getInstance().tick(mc);
	}

	public static TradeChoiceConfig getConfig() {
		return config;
	}

	public static TradeAlertManager getAlertManager() {
		return alertManager;
	}
}
