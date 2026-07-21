package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.platform.FabricPlatform;
import com.tradechoice.client.platform.Platforms;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TradeChoiceFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		TradeChoiceMod.LOGGER.info("[tradechoice] fabric bootstrap starting");
		Platforms.set(new FabricPlatform());
		TradeChoiceClient.init();
		ClientTickEvents.END_CLIENT_TICK.register(TradeChoiceClient::tick);
		TradeChoiceMod.LOGGER.info("[tradechoice] fabric bootstrap complete");
	}
}
