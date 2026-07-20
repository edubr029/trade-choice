package com.tradechoice.client.gui;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.TradeChoiceClient;
import com.tradechoice.client.config.WantedTrade;
import com.tradechoice.client.mixin.VillagerTradeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TradeWishlistScreen extends Screen {

	private static final int ROWS_PER_PAGE = 10;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_SPACING = 22;

	private final MerchantScreen parent;
	private final String professionKey;
	private final List<WantedTrade> entries = new ArrayList<>();
	private int page = 0;

	public TradeWishlistScreen(MerchantScreen parent, String professionKey) {
		super(Component.literal("\u2605 Wanted Trades"));
		this.parent = parent;
		this.professionKey = professionKey;
	}

	@Override
	protected void init() {
		// Build entries once per screen lifetime.
		if (entries.isEmpty()) {
			buildEntries();
		}

		int startIndex = page * ROWS_PER_PAGE;
		int endIndex = Math.min(startIndex + ROWS_PER_PAGE, entries.size());

		int xLeft = width / 2 - BUTTON_WIDTH - 4;
		int xRight = width / 2 + 4;
		int yStart = 50;

		for (int i = startIndex; i < endIndex; i++) {
			int rowIdx = i - startIndex;
			int x = rowIdx % 2 == 0 ? xLeft : xRight;
			int y = yStart + (rowIdx / 2) * BUTTON_SPACING;

			WantedTrade entry = entries.get(i);
			boolean marked = TradeChoiceClient.getConfig().isMarked(entry);
			String label = (marked ? "\u2713 " : "  ") + prettyLabel(entry);

			addRenderableWidget(Button.builder(
				Component.literal(label),
				btn -> {
					TradeChoiceClient.getConfig().toggle(entry);
					rebuildWidgets();
				}
			).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		}

		// Navigation row
		int navY = height - 60;
		int navXCenter = width / 2;
		if (page > 0) {
			addRenderableWidget(Button.builder(
				Component.literal("< Prev"),
				b -> { page--; rebuildWidgets(); }
			).bounds(navXCenter - 160, navY, 70, 20).build());
		}
		if ((page + 1) * ROWS_PER_PAGE < entries.size()) {
			addRenderableWidget(Button.builder(
				Component.literal("Next >"),
				b -> { page++; rebuildWidgets(); }
			).bounds(navXCenter + 90, navY, 70, 20).build());
		}

		int totalPages = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
		addRenderableWidget(Button.builder(
			Component.literal((page + 1) + "/" + totalPages),
			b -> {}
		).bounds(navXCenter - 35, navY, 70, 20).build());

		// Done button
		addRenderableWidget(Button.builder(
			Component.literal("Done"),
			b -> onClose()
		).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Dark semi-transparent overlay across the whole screen.
		g.fill(0, 0, width, height, 0xC0101010);
		// Title at top.
		g.centeredText(font, this.title, width / 2, 18, 0xFFFFD700);
		// Subtitle with profession name.
		g.centeredText(font, Component.literal(prettyProfessionName()), width / 2, 32, 0xFFCCCCCC);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(parent);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	/** Populate entries from the TRADE_SET registry for this profession. */
	private void buildEntries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (professionKey == null) return;

		// Find the profession registry entry.
		Optional<Holder.Reference<VillagerProfession>> profOpt = mc.level.registryAccess()
			.lookupOrThrow(Registries.VILLAGER_PROFESSION)
			.get(Identifier.tryParse(professionKey));
		if (profOpt.isEmpty()) {
			TradeChoiceMod.LOGGER.warn("[trade-choice] No villager profession found for key {}", professionKey);
			return;
		}
		VillagerProfession prof = profOpt.get().value();

		// Lazy-load tradeable enchantments if we encounter an enchanted_book trade.
		HolderSet.Named<Enchantment> tradeableEnchants = null;

		// For each level 1..5, fetch the TradeSet and iterate its trades.
		for (int level = 1; level <= 5; level++) {
			ResourceKey<TradeSet> setKey = prof.getTrades(level);
			if (setKey == null) continue;
			Optional<TradeSet> setOpt = mc.level.registryAccess()
				.lookupOrThrow(Registries.TRADE_SET)
				.getOptional(setKey);
			if (setOpt.isEmpty()) continue;
			TradeSet tradeSet = setOpt.get();
			HolderSet<VillagerTrade> trades = tradeSet.getTrades();
			for (Holder<VillagerTrade> holder : trades) {
				VillagerTrade trade = holder.value();
				ItemStackTemplate gives = ((VillagerTradeAccessor) (Object) trade).tradechoice$getGives();
				Holder<Item> itemHolder = gives.item();
				Item item = itemHolder.value();
				Identifier itemId = itemHolder.unwrapKey()
					.map(ResourceKey::identifier)
					.orElse(Identifier.withDefaultNamespace("unknown"));

				if (item == Items.ENCHANTED_BOOK) {
					if (tradeableEnchants == null) {
						tradeableEnchants = mc.level.registryAccess()
							.lookupOrThrow(Registries.ENCHANTMENT)
							.getOrThrow(EnchantmentTags.TRADEABLE);
					}
					for (Holder<Enchantment> e : tradeableEnchants) {
						String enchId = e.unwrapKey()
							.map(ResourceKey::identifier)
							.map(Identifier::toString)
							.orElse(null);
						if (enchId == null) continue;
						WantedTrade w = new WantedTrade(professionKey, itemId.toString(), enchId, 0);
						if (!entries.contains(w)) entries.add(w);
					}
				} else {
					WantedTrade w = new WantedTrade(professionKey, itemId.toString(), null, 0);
					if (!entries.contains(w)) entries.add(w);
				}
			}
		}
	}

	private String prettyLabel(WantedTrade w) {
		if (w.getEnchantmentId() != null) {
			return prettyName(w.getEnchantmentId()) + " (Book)";
		}
		return prettyName(w.getItemId());
	}

	private String prettyProfessionName() {
		return prettyName(professionKey);
	}

	private String prettyName(String id) {
		Identifier parsed = Identifier.tryParse(id);
		if (parsed == null) return id;
		String path = parsed.getPath();
		StringBuilder sb = new StringBuilder();
		boolean capitalize = true;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '_') {
				sb.append(' ');
				capitalize = true;
			} else {
				if (capitalize) {
					sb.append(Character.toUpperCase(c));
					capitalize = false;
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}
}
