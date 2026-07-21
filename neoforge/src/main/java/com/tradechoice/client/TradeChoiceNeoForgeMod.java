package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = "tradechoice", dist = Dist.CLIENT)
public class TradeChoiceNeoForgeMod {
    public TradeChoiceNeoForgeMod(IEventBus modBus) {
        TradeChoiceMod.LOGGER.info("tradechoice neoforge entrypoint");
    }
}
