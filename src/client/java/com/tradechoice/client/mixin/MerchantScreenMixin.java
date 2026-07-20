package com.tradechoice.client.mixin;

import com.tradechoice.client.TradeChoiceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

	@Shadow
	private int shopItem;

	@Unique
	private String tradeChoice$profession;

	private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void tradeChoice$onInit(CallbackInfo ci) {
		tradeChoice$profession = tradeChoice$detectProfession();
		if (tradeChoice$profession != null) {
			MerchantOffers offers = getMenu().getOffers();
			TradeChoiceClient.getAlertManager().onScreenOpen();
			TradeChoiceClient.getAlertManager().checkAndAlert(offers, tradeChoice$profession);
		}
	}

	@Inject(method = "extractContents", at = @At("TAIL"))
	private void tradeChoice$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (tradeChoice$profession == null) return;

		MerchantOffers offers = getMenu().getOffers();
		if (offers.isEmpty()) return;

		int visibleCount = Math.min(7, offers.size() - shopItem);
		for (int i = 0; i < visibleCount; i++) {
			int offerIndex = shopItem + i;
			if (offerIndex >= offers.size()) break;

			MerchantOffer offer = offers.get(offerIndex);
			if (TradeChoiceClient.getAlertManager().isOfferMarked(offer, tradeChoice$profession)) {
				int markerX = leftPos + 5 - 7;
				int markerY = topPos + 18 + (i * 20) + 7;
				graphics.fill(markerX, markerY, markerX + 5, markerY + 5, 0xFFFFD700);
			}
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tradeChoice$onMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != 0 || !event.hasShiftDown() || tradeChoice$profession == null) return;

		MerchantOffers offers = getMenu().getOffers();
		if (offers.isEmpty()) return;

		double mouseX = event.x();
		double mouseY = event.y();

		if (mouseX >= leftPos + 5 && mouseX <= leftPos + 93 && mouseY >= topPos + 18) {
			int relY = (int) (mouseY - (topPos + 18));
			int slotIndex = relY / 20;
			if (slotIndex < 0 || slotIndex >= 7) return;

			int offerIndex = shopItem + slotIndex;
			if (offerIndex >= 0 && offerIndex < offers.size()) {
				TradeChoiceClient.getAlertManager().toggleMark(tradeChoice$profession, offers.get(offerIndex));
				cir.setReturnValue(true);
			}
		}
	}

	@Unique
	private String tradeChoice$detectProfession() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;

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
