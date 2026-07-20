package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.config.TradeChoiceConfig;
import net.fabricmc.api.ClientModInitializer;

public class TradeChoiceClient implements ClientModInitializer {

	private static TradeChoiceConfig config;
	private static TradeAlertManager alertManager;

	@Override
	public void onInitializeClient() {
		config = new TradeChoiceConfig();
		config.load();
		alertManager = new TradeAlertManager(config);
		TradeChoiceMod.LOGGER.info("[trade-choice] Client initialized, loaded {} choices", config.getChoices().size());
	}

	public static TradeChoiceConfig getConfig() {
		return config;
	}

	public static TradeAlertManager getAlertManager() {
		return alertManager;
	}
}
