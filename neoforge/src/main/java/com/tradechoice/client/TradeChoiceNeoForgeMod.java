package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.platform.NeoForgePlatform;
import com.tradechoice.client.platform.Platforms;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "tradechoice", dist = Dist.CLIENT)
public class TradeChoiceNeoForgeMod {

	public TradeChoiceNeoForgeMod(IEventBus modBus) {
		TradeChoiceMod.LOGGER.info("[tradechoice] neoforge bootstrap starting");
		Platforms.set(new NeoForgePlatform());
		TradeChoiceClient.init();
		NeoForge.EVENT_BUS.addListener(this::onClientTickPost);
		TradeChoiceMod.LOGGER.info("[tradechoice] neoforge bootstrap complete");
	}

	private void onClientTickPost(ClientTickEvent.Post event) {
		TradeChoiceClient.tick(Minecraft.getInstance());
	}
}
