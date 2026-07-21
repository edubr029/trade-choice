package com.tradechoice.client.mixin;

import com.tradechoice.client.TradeChoiceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
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

import com.tradechoice.client.cycling.AutoSearchDriver;
import com.tradechoice.client.platform.Platforms;

import java.util.List;
import java.util.Optional;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

	@Shadow
	private int shopItem;

	@Unique
	private String tradeChoice$profession;

	@Unique
	private Button tradeChoice$autoSearchButton;

	private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void tradeChoice$onInit(CallbackInfo ci) {
		tradeChoice$autoSearchButton = null;
		Villager villager = tradeChoice$findNearestVillager();
		tradeChoice$profession = tradeChoice$professionOf(villager);
		if (tradeChoice$profession != null) {
			MerchantOffers offers = getMenu().getOffers();
			TradeChoiceClient.getAlertManager().onScreenOpen();
			TradeChoiceClient.getAlertManager().checkAndAlert(offers, tradeChoice$profession);

			if (villager != null) {
				int autoW = 80;
				int wantedW = 20;
				int h = 20;
				int y = topPos - 22;
				int wantedX = leftPos + imageWidth - wantedW;

				if (villager.getVillagerData().level() < VillagerData.MAX_VILLAGER_LEVEL) {
					int autoX = wantedX - autoW - 4;

					AutoSearchDriver driver = AutoSearchDriver.getInstance();
					tradeChoice$autoSearchButton = Button.builder(
						Component.literal(driver.isRunning() ? "Stop Search" : "Auto Search"),
						btn -> {
							if (driver.isRunning()) {
								driver.stop("Stopped by user");
							} else {
								driver.start(tradeChoice$profession);
							}
						}
					).bounds(autoX, y, autoW, h).build();
					tradeChoice$applyAutoSearchButtonState(driver, false);
					addRenderableWidget(tradeChoice$autoSearchButton);
				}

				SpriteIconButton wantedButton = SpriteIconButton.builder(
					Component.literal("Wanted"),
					btn -> Minecraft.getInstance().setScreenAndShow(
						new com.tradechoice.client.gui.TradeWishlistScreen(
							(MerchantScreen) (Object) this,
							tradeChoice$profession
						)
					),
					true
				).size(wantedW, h)
				 .sprite(Identifier.fromNamespaceAndPath("minecraft", "icon/search"), 12, 12)
				 .build();
				wantedButton.setPosition(wantedX, y);
				addRenderableWidget(wantedButton);
			}
		}
	}

	@Inject(method = "extractContents", at = @At("TAIL"))
	private void tradeChoice$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (tradeChoice$profession == null) return;

		MerchantOffers offers = getMenu().getOffers();
		if (!offers.isEmpty()) {
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

		AutoSearchDriver driver = AutoSearchDriver.getInstance();

		if (tradeChoice$autoSearchButton != null) {
			String expected = driver.isRunning() ? "Stop Search" : "Auto Search";
			if (!expected.equals(tradeChoice$autoSearchButton.getMessage().getString())) {
				tradeChoice$autoSearchButton.setMessage(Component.literal(expected));
			}
			tradeChoice$applyAutoSearchButtonState(driver, true);
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
	private void tradeChoice$applyAutoSearchButtonState(AutoSearchDriver driver, boolean recheckOnChange) {
		if (tradeChoice$autoSearchButton == null) return;

		boolean tradeCyclingLoaded = Platforms.get().isModLoaded("trade_cycling");
		boolean running = driver.isRunning();
		boolean hasWanted = !TradeChoiceClient.getConfig()
				.getChoicesForProfession(tradeChoice$profession).isEmpty();

		boolean targetActive;
		String tooltipMsg;
		if (!tradeCyclingLoaded) {
			targetActive = false;
			tooltipMsg = "Install the Trade Cycling mod to auto search";
		} else if (running || hasWanted) {
			targetActive = true;
			tooltipMsg = null;
		} else {
			targetActive = false;
			tooltipMsg = "Mark a wanted trade first (shift-click any trade row)";
		}

		if (recheckOnChange && tradeChoice$autoSearchButton.active == targetActive) return;

		tradeChoice$autoSearchButton.active = targetActive;
		if (tooltipMsg == null) {
			tradeChoice$autoSearchButton.setTooltip(null);
		} else {
			tradeChoice$autoSearchButton.setTooltip(Tooltip.create(Component.literal(tooltipMsg)));
		}
	}

	@Unique
	private Villager tradeChoice$findNearestVillager() {		Minecraft mc = Minecraft.getInstance();
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
		return closest;
	}

	@Unique
	private String tradeChoice$professionOf(Villager villager) {
		if (villager == null) return null;
		Holder<VillagerProfession> profHolder = villager.getVillagerData().profession();
		Optional<ResourceKey<VillagerProfession>> keyOpt = profHolder.unwrapKey();
		return keyOpt.map(k -> k.identifier().toString()).orElse(null);
	}
}
