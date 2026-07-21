package com.tradechoice.client;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.config.TradeChoiceConfig;
import com.tradechoice.client.config.WantedTrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

public class TradeAlertManager {

	private final TradeChoiceConfig config;
	private boolean alertedThisOpen;
	private boolean matchedSinceLastConsume;
	private int lastOffersHash;

	public TradeAlertManager(TradeChoiceConfig config) {
		this.config = config;
	}

	public void onScreenOpen() {
		alertedThisOpen = false;
		matchedSinceLastConsume = false;
		lastOffersHash = 0;
	}

	public boolean checkAndAlert(MerchantOffers offers, String profession) {
		if (offers == null || profession == null) return false;

		int currentHash = computeOffersHash(offers);
		if (currentHash == lastOffersHash && alertedThisOpen) return false;
		lastOffersHash = currentHash;

		List<WantedTrade> wanted = config.getChoicesForProfession(profession);
		if (wanted.isEmpty()) return false;

		for (MerchantOffer offer : offers) {
			ItemStack result = offer.getResult();
			for (WantedTrade w : wanted) {
				if (matchesItem(result, w)) {
					WantedTrade found = createFromStack(profession, result);
					TradeChoiceMod.LOGGER.info(
							"[trade-choice] MATCH found: profession={} | wanted item={} enchId={} enchLvl={} | found item={} enchId={} enchLvl={}",
							profession,
							w.getItemId(),
							w.getEnchantmentId() == null ? "<any>" : w.getEnchantmentId(),
							w.getEnchantmentLevel() == 0 ? "<any>" : w.getEnchantmentLevel(),
							found.getItemId(),
							found.getEnchantmentId() == null ? "<none>" : found.getEnchantmentId(),
							found.getEnchantmentLevel() == 0 ? "<none>" : found.getEnchantmentLevel());
					playAlert();
					alertedThisOpen = true;
					matchedSinceLastConsume = true;
					return true;
				}
			}
		}
		return false;
	}

	public void logWantedFor(String profession) {
		List<WantedTrade> wanted = config.getChoicesForProfession(profession);
		if (wanted.isEmpty()) {
			TradeChoiceMod.LOGGER.info(
					"[trade-choice] Auto-search target: profession={} marks=0 (no wanted entries)",
					profession);
			return;
		}
		TradeChoiceMod.LOGGER.info(
				"[trade-choice] Auto-search target: profession={} marks={}",
				profession, wanted.size());
		for (WantedTrade w : wanted) {
			TradeChoiceMod.LOGGER.info(
					"[trade-choice]   - item={} enchId={} enchLvl={}",
					w.getItemId(),
					w.getEnchantmentId() == null ? "<any>" : w.getEnchantmentId(),
					w.getEnchantmentLevel() == 0 ? "<any>" : w.getEnchantmentLevel());
		}
	}

	public boolean isOfferMarked(MerchantOffer offer, String profession) {
		WantedTrade probe = createFromStack(profession, offer.getResult());
		return config.isMarked(probe);
	}

	public void toggleMark(String profession, MerchantOffer offer) {
		WantedTrade trade = createFromStack(profession, offer.getResult());
		config.toggle(trade);
		TradeChoiceMod.LOGGER.info("[trade-choice] Toggled mark: profession={}, item={}", profession, trade.getItemId());
	}

	public boolean consumeRecentlyMatched() {
		boolean result = matchedSinceLastConsume;
		matchedSinceLastConsume = false;
		return result;
	}

	public static WantedTrade createFromStack(String profession, ItemStack stack) {
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String enchantmentId = null;
		int enchantmentLevel = 0;

		if (stack.is(Items.ENCHANTED_BOOK)) {
			EnchantmentUtil.EnchantmentInfo info = EnchantmentUtil.getFirstStoredEnchantment(stack);
			if (info != null) {
				enchantmentId = info.id();
				enchantmentLevel = info.level();
			}
		}

		return new WantedTrade(profession, itemId, enchantmentId, enchantmentLevel);
	}

	private boolean matchesItem(ItemStack stack, WantedTrade wanted) {
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (!wanted.getItemId().equals(itemId)) return false;

		if (wanted.getEnchantmentId() != null) {
			EnchantmentUtil.EnchantmentInfo info = EnchantmentUtil.getFirstStoredEnchantment(stack);
			if (info == null) return false;
			if (!wanted.getEnchantmentId().equals(info.id())) return false;
			if (wanted.getEnchantmentLevel() != 0 && wanted.getEnchantmentLevel() != info.level()) return false;
		}
		return true;
	}

	private void playAlert() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
	}

	private int computeOffersHash(MerchantOffers offers) {
		int hash = offers.size();
		for (MerchantOffer offer : offers) {
			hash = 31 * hash + offer.getResult().getItem().hashCode();
			if (offer.getResult().is(Items.ENCHANTED_BOOK)) {
				EnchantmentUtil.EnchantmentInfo info = EnchantmentUtil.getFirstStoredEnchantment(offer.getResult());
				if (info != null) {
					hash = 31 * hash + info.hashCode();
				}
			}
		}
		return hash;
	}
}
