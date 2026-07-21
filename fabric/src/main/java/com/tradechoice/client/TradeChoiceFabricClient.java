package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import net.fabricmc.api.ClientModInitializer;

public class TradeChoiceFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TradeChoiceMod.LOGGER.info("tradechoice fabric entrypoint");
    }
}
