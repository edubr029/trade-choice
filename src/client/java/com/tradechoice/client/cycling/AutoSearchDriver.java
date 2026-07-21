package com.tradechoice.client.cycling;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.TradeChoiceClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class AutoSearchDriver {

	private static final String CYCLE_PACKET_CLASS = "de.maxhenkel.tradecycling.net.CycleTradesPacket";
	private static final AutoSearchDriver INSTANCE = new AutoSearchDriver();
	private static final int MAX_CYCLES = 2000;

	public static AutoSearchDriver getInstance() { return INSTANCE; }

	private boolean running = false;
	private int cyclesSent = 0;
	private String profession = null;
	private Class<?> cachedPacketClass = null;
	private boolean awaitingReply = false;

	private AutoSearchDriver() {}

	public boolean isRunning() { return running; }

	public void start(String profession) {
		if (running) return;
		this.profession = profession;
		running = true;
		cyclesSent = 0;
		awaitingReply = false;
		TradeChoiceMod.LOGGER.info(
				"[trade-choice] Auto-search START profession={} maxCycles={}",
				profession, MAX_CYCLES);
		TradeChoiceClient.getAlertManager().logWantedFor(profession);
		TradeChoiceClient.getAlertManager().consumeRecentlyMatched();
	}

	public void stop(String reason) {
		if (!running) return;
		running = false;
		TradeChoiceMod.LOGGER.info("[trade-choice] Auto-search STOP after {} cycles (reason={})", cyclesSent, reason);
		cyclesSent = 0;
		awaitingReply = false;
		profession = null;
	}

	public void notifyMerchantReplyReceived() {
		awaitingReply = false;
	}

	public void tick(Minecraft mc) {
		if (!running) return;

		if (!(mc.gui.screen() instanceof MerchantScreen)) {
			TradeChoiceMod.LOGGER.info(
					"[trade-choice] Auto-search: MerchantScreen gone (actual={}); stopping",
					mc.gui.screen());
			stop("MerchantScreen closed");
			return;
		}
		if (TradeChoiceClient.getAlertManager().consumeRecentlyMatched()) {
			stop("Match found!");
			return;
		}
		if (cyclesSent >= MAX_CYCLES) {
			stop("max cycles reached, no match");
			return;
		}
		if (awaitingReply) return;

		sendCyclePacket();
		cyclesSent++;
		awaitingReply = true;
	}

	private void sendCyclePacket() {
		try {
			if (cachedPacketClass == null) {
				cachedPacketClass = Class.forName(CYCLE_PACKET_CLASS);
			}
			Object payload = cachedPacketClass.getDeclaredConstructor().newInstance();
			if (payload instanceof CustomPacketPayload cpp) {
				ClientPlayNetworking.send(cpp);
			} else {
				TradeChoiceMod.LOGGER.error("[trade-choice] Reflected payload {} is not a CustomPacketPayload ({})",
						CYCLE_PACKET_CLASS, payload.getClass());
				stop("packet class cast failure");
			}
		} catch (ClassNotFoundException e) {
			TradeChoiceMod.LOGGER.error("[trade-choice] Trade Cycling mod class not found at runtime despite isModLoaded true", e);
			stop("trade_cycling class missing");
		} catch (ReflectiveOperationException e) {
			TradeChoiceMod.LOGGER.error("[trade-choice] Failed to reflectively construct CycleTradesPacket", e);
			stop("reflection failure");
		} catch (Throwable t) {
			TradeChoiceMod.LOGGER.error("[trade-choice] Unexpected error sending cycle packet", t);
			stop("unexpected error");
		}
	}
}
