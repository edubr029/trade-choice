package com.tradechoice.client.mixin;

import com.tradechoice.client.TradeChoiceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "handleMerchantOffers", at = @At("TAIL"))
	private void tradeChoice$onMerchantOffers(ClientboundMerchantOffersPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		if (!(mc.gui.screen() instanceof MerchantScreen)) return;

		String profession = tradeChoice$findNearestProfession(mc);
		if (profession == null) return;

		MerchantOffers offers = packet.getOffers();
		TradeChoiceClient.getAlertManager().checkAndAlert(offers, profession);
	}

	private static String tradeChoice$findNearestProfession(Minecraft mc) {
		List<Villager> villagers = mc.level.getEntitiesOfClass(
				Villager.class,
				mc.player.getBoundingBox().inflate(6.0)
		);

		Villager closest = null;
		double minDist = Double.MAX_VALUE;
		for (Villager v : villagers) {
			double dist = v.distanceToSqr(mc.player);
			if (dist < minDist) {
				minDist = dist;
				closest = v;
			}
		}

		if (closest == null) return null;

		Holder<VillagerProfession> profHolder = closest.getVillagerData().profession();
		Optional<ResourceKey<VillagerProfession>> keyOpt = profHolder.unwrapKey();
		return keyOpt.map(k -> k.identifier().toString()).orElse(null);
	}
}
