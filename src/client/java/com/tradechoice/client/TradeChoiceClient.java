package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.config.TradeChoiceConfig;
import com.tradechoice.client.cycling.AutoSearchDriver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TradeChoiceClient implements ClientModInitializer {

	private static TradeChoiceConfig config;
	private static TradeAlertManager alertManager;

	@Override
	public void onInitializeClient() {
		config = new TradeChoiceConfig();
		config.load();
		alertManager = new TradeAlertManager(config);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> AutoSearchDriver.getInstance().tick(mc));
		TradeChoiceMod.LOGGER.info(
				"[trade-choice] Client initialized, loaded {} choices (auto-search: 1 reply-in-flight, max 2000 cycles)",
				config.getChoices().size());
	}

	public static TradeChoiceConfig getConfig() { return config; }
	public static TradeAlertManager getAlertManager() { return alertManager; }
}
