package com.tradechoice.client.gui;

import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.TradeChoiceClient;
import com.tradechoice.client.config.WantedTrade;
import com.tradechoice.client.cycling.AutoSearchDriver;
import com.tradechoice.client.mixin.VillagerTradeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TradeWishlistScreen extends Screen {

	private static final int LIST_WIDTH = 250;
	private static final int ROW_HEIGHT = 22;
	private static final int HEADER_HEIGHT = 52;
	private static final int FOOTER_HEIGHT = 32;

	private static final int LEVEL_REGION_WIDTH = 64;

	private final MerchantScreen parent;
	private final String professionKey;
	private final List<WantedTrade> entries = new ArrayList<>();
	private final List<ItemStack> iconStacks = new ArrayList<>();
	private final List<Integer> entryMaxLevels = new ArrayList<>();
	private final Map<String, Integer> chosenLevelByEnchant = new HashMap<>();
	private boolean unavailable = false;
	private String unavailableReason = null;
	private int scrollOffset = 0;
	private EditBox searchBox;
	private String searchText = "";
	private boolean searchWasFocused = false;
	private boolean scrollDragging = false;

	public TradeWishlistScreen(MerchantScreen parent, String professionKey) {
		super(Component.literal("\u2605 Wanted Trades"));
		this.parent = parent;
		this.professionKey = professionKey;
	}

	@Override
	protected void init() {
		if (entries.isEmpty()) {
			buildEntries();
			buildIconStacks();
		}

		List<WantedTrade> markedChoices = TradeChoiceClient.getConfig().getChoicesForProfession(professionKey);
		if (!markedChoices.isEmpty()) {
			WantedTrade firstMarked = markedChoices.get(0);
			if (firstMarked.getEnchantmentId() != null && firstMarked.getEnchantmentLevel() > 0) {
				chosenLevelByEnchant.putIfAbsent(firstMarked.getEnchantmentId(), firstMarked.getEnchantmentLevel());
			}
		}

		searchBox = new EditBox(font, width / 2 - 125, 26, 250, 18, Component.literal("Search wanted trades"));
		searchBox.setMaxLength(128);
		searchBox.setHint(Component.literal("Search..."));
		searchBox.setResponder(text -> {
			if (Objects.equals(text, searchText)) return;
			searchText = text;
			searchWasFocused = searchBox.isFocused();
			TradeWishlistScreen.this.rebuildWidgets();
		});
		searchBox.setValue(searchText);
		addRenderableWidget(searchBox);
		if (searchWasFocused) {
			this.setFocused(searchBox);
			searchBox.moveCursorToEnd(false);
		}

		List<WantedTrade> filtered = filteredEntries();
		int listX = width / 2 - 125;
		int listY = HEADER_HEIGHT;
		int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
		int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
		scrollOffset = clamp(scrollOffset, 0, maxScrollOffset(filtered.size(), visibleRows));

		if (!unavailable && !filtered.isEmpty()) {
			int startIndex = scrollOffset / ROW_HEIGHT;
			int rowOffset = scrollOffset % ROW_HEIGHT;
			int rowsToRender = Math.min(filtered.size() - startIndex, visibleRows + (rowOffset > 0 ? 1 : 0));
			for (int i = 0; i < rowsToRender; i++) {
				int entryIndex = startIndex + i;
				WantedTrade base = filtered.get(entryIndex);
				int originalIdx = entries.indexOf(base);
				int maxLvl = entryMaxLevels.get(originalIdx);
				int chosenLevel = base.getEnchantmentId() != null
					? chosenLevelByEnchant.getOrDefault(base.getEnchantmentId(), 0)
					: 0;
				WantedTrade display = new WantedTrade(
					base.getProfession(), base.getItemId(),
					base.getEnchantmentId(), chosenLevel);
				boolean marked = TradeChoiceClient.getConfig().isMarked(display);
				addRenderableWidget(new WishlistRowWidget(
					listX,
					listY + i * ROW_HEIGHT - rowOffset,
					LIST_WIDTH,
					ROW_HEIGHT,
					display,
					iconStacks.get(originalIdx),
					marked,
					maxLvl
				));
			}
		}

		addRenderableWidget(
			Button.builder(
				Component.literal("Done"),
				b -> onClose()
			).bounds(width / 2 - 100, height - 24, 200, 18).build()
		);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0xC0101010);
		g.centeredText(font, this.title, width / 2, 4, 0xFFFFD700);
		g.centeredText(font, Component.literal(prettyProfessionName()), width / 2, 16, 0xFFCCCCCC);
		if (unavailable) {
			g.centeredText(
				font,
				Component.literal(unavailableReason == null ? "Unavailable." : unavailableReason),
				width / 2, height / 2 - 10, 0xFFFF5555
			);
		} else if (entries.isEmpty()) {
			g.centeredText(
				font,
				Component.literal("No trades enumerated for this profession."),
				width / 2, height / 2 - 10, 0xFFAAAAAA
			);
		} else if (filteredEntries().isEmpty()) {
			g.centeredText(
				font,
				Component.literal("No matches for '" + searchText + "'"),
				width / 2, height / 2 - 10, 0xFFAAAAAA
			);
		}

		int listX = width / 2 - 125;
		int listY = HEADER_HEIGHT;
		int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
		int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
		List<WantedTrade> filtered = filteredEntries();
		
		if (filtered.size() > visibleRows) {
			int trackX0 = listX + LIST_WIDTH + 4;
			int trackX1 = trackX0 + 6;
			int trackY0 = listY;
			int trackY1 = listY + listHeight;
			
			int thumbHeight = Math.max(8, listHeight * visibleRows / Math.max(1, filtered.size()));
			int maxScroll = maxScrollOffset(filtered.size(), visibleRows);
			int thumbY = trackY0 + (maxScroll == 0 ? 0 : (listHeight - thumbHeight) * scrollOffset / maxScroll);
			
			g.fill(trackX0, trackY0, trackX1, trackY1, 0xFF202020);
			g.fill(trackX0, thumbY, trackX1, thumbY + thumbHeight, 0xFFA0A0A0);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		int listX = width / 2 - 125;
		int listY = HEADER_HEIGHT;
		int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
		int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
		List<WantedTrade> filtered = filteredEntries();
		
		if (filtered.size() > visibleRows) {
			int trackX0 = listX + LIST_WIDTH + 4;
			int trackX1 = trackX0 + 6;
			int trackY0 = listY;
			int trackY1 = listY + listHeight;
			
			if (event.x() >= trackX0 && event.x() <= trackX1 && event.y() >= trackY0 && event.y() <= trackY1) {
				scrollDragging = true;
				int thumbHeight = Math.max(8, listHeight * visibleRows / Math.max(1, filtered.size()));
				int maxScroll = maxScrollOffset(filtered.size(), visibleRows);
				int trackHeight = listHeight - thumbHeight;
				if (trackHeight > 0) {
					double frac = clamp((int)(event.y() - trackY0 - thumbHeight / 2), 0, trackHeight) / (double) trackHeight;
					int newScroll = (int) Math.round(frac * maxScroll);
					if (newScroll != scrollOffset) {
						scrollOffset = newScroll;
						rebuildWidgets();
					}
				}
				return true;
			}
		}
		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && scrollDragging) {
			scrollDragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (scrollDragging) {
			int listY = HEADER_HEIGHT;
			int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
			int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
			List<WantedTrade> filtered = filteredEntries();
			
			int thumbHeight = Math.max(8, listHeight * visibleRows / Math.max(1, filtered.size()));
			int maxScroll = maxScrollOffset(filtered.size(), visibleRows);
			int trackHeight = listHeight - thumbHeight;
			if (trackHeight > 0) {
				double frac = clamp((int)(event.y() - listY - thumbHeight / 2), 0, trackHeight) / (double) trackHeight;
				int newScroll = (int) Math.round(frac * maxScroll);
				if (newScroll != scrollOffset) {
					scrollOffset = newScroll;
					rebuildWidgets();
				}
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int listX = width / 2 - 125;
		int listY = HEADER_HEIGHT;
		int listHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
		int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
		List<WantedTrade> filtered = filteredEntries();
		if (unavailable || filtered.isEmpty() || mouseX < listX || mouseX >= listX + LIST_WIDTH || mouseY < listY || mouseY >= listY + listHeight) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		int nextOffset = scrollOffset - (int) Math.round(verticalAmount * ROW_HEIGHT);
		nextOffset = clamp(nextOffset, 0, maxScrollOffset(filtered.size(), visibleRows));
		if (nextOffset == scrollOffset) {
			return true;
		}
		scrollOffset = nextOffset;
		rebuildWidgets();
		return true;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(parent);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	private void buildIconStacks() {
		iconStacks.clear();
		for (WantedTrade entry : entries) {
			iconStacks.add(iconStack(entry));
		}
	}

	private ItemStack iconStack(WantedTrade entry) {
		Identifier id = Identifier.tryParse(entry.getItemId());
		if (id == null) {
			return new ItemStack(Items.BARRIER);
		}
		Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(id);
		return item.<ItemStack>map(ItemStack::new).orElseGet(() -> new ItemStack(Items.BARRIER));
	}

	private int maxScrollOffset(int entryCount, int visibleRows) {
		return Math.max(0, entryCount * ROW_HEIGHT - visibleRows * ROW_HEIGHT);
	}

	private List<WantedTrade> filteredEntries() {
		List<WantedTrade> result;
		if (searchText == null || searchText.isBlank()) {
			result = new ArrayList<>(entries);
		} else {
			String target = searchText.toLowerCase(Locale.ROOT);
			result = entries.stream()
				.filter(w -> {
					String label = prettyLabel(w).toLowerCase(Locale.ROOT);
					String itemId = w.getItemId() == null ? "" : w.getItemId().toLowerCase(Locale.ROOT);
					String enchantmentId = w.getEnchantmentId() == null ? "" : w.getEnchantmentId().toLowerCase(Locale.ROOT);
					return label.contains(target) || itemId.contains(target) || enchantmentId.contains(target);
				})
				.collect(Collectors.toCollection(ArrayList::new));
		}
		return buoyMarkedToTop(result);
	}

	private List<WantedTrade> buoyMarkedToTop(List<WantedTrade> list) {
		WantedTrade base = markedBaseIfAny();
		if (base == null || !list.remove(base)) return list;
		list.add(0, base);
		return list;
	}

	private WantedTrade markedBaseIfAny() {
		List<WantedTrade> marked = TradeChoiceClient.getConfig().getChoicesForProfession(professionKey);
		if (marked.isEmpty()) return null;
		WantedTrade m = marked.get(0);
		return new WantedTrade(m.getProfession(), m.getItemId(), m.getEnchantmentId(), 0);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Populate entries from the TRADE_SET registry for this profession.
	 * TRADE_SET is worldgen-scoped: it is NOT in the client-side
	 * {@code mc.level.registryAccess()} (which only contains sync'd registries).
	 * On integrated single-player the local {@link IntegratedServer} shares our
	 * process and exposes the full worldgen registries via
	 * {@code server.registryAccess()}; remote/dedicated servers do not, so we
	 * surface a fallback message inside the panel instead of crashing.
	 */
	private void buildEntries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (professionKey == null) return;

		RegistryAccess regAccess = null;
		if (mc.hasSingleplayerServer()) {
			IntegratedServer server = mc.getSingleplayerServer();
			if (server != null) regAccess = server.registryAccess();
		}
		if (regAccess == null) {
			regAccess = mc.level.registryAccess();
		}
		if (regAccess == null) {
			unavailable = true;
			unavailableReason = "No registry access available.";
			return;
		}

		Optional<Holder.Reference<VillagerProfession>> profOpt;
		try {
			profOpt = regAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION)
				.get(Identifier.tryParse(professionKey));
		} catch (IllegalStateException e) {
			TradeChoiceMod.LOGGER.warn("[trade-choice] VILLAGER_PROFESSION registry unavailable", e);
			unavailable = true;
			unavailableReason = "Profession registry not available on this server.";
			return;
		}
		if (profOpt.isEmpty()) {
			TradeChoiceMod.LOGGER.warn("[trade-choice] No villager profession found for key {}", professionKey);
			unavailable = true;
			unavailableReason = "Unknown profession: " + professionKey;
			return;
		}
		VillagerProfession prof = profOpt.get().value();

		Registry<TradeSet> tradeSetRegistry;
		try {
			tradeSetRegistry = regAccess.lookupOrThrow(Registries.TRADE_SET);
		} catch (IllegalStateException e) {
			TradeChoiceMod.LOGGER.warn("[trade-choice] TRADE_SET registry unavailable on this client", e);
			unavailable = true;
			unavailableReason = "Trade list registry not available on this server. Open the wishlist on a single-player world.";
			return;
		}

		Registry<Enchantment> enchantRegistry = null;
		HolderSet.Named<Enchantment> tradeableEnchants = null;

		for (int level = 1; level <= 5; level++) {
			ResourceKey<TradeSet> setKey = prof.getTrades(level);
			if (setKey == null) continue;
			Optional<TradeSet> setOpt = tradeSetRegistry.getOptional(setKey);
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
					if (enchantRegistry == null) {
						try {
							enchantRegistry = regAccess.lookupOrThrow(Registries.ENCHANTMENT);
						} catch (IllegalStateException e) {
							TradeChoiceMod.LOGGER.warn("[trade-choice] ENCHANTMENT registry unavailable; listing enchanted_book generically", e);
							WantedTrade w = new WantedTrade(professionKey, itemId.toString(), null, 0);
							if (!entries.contains(w)) {
								entries.add(w);
								entryMaxLevels.add(1);
							}
							continue;
						}
					}
					if (tradeableEnchants == null) {
						tradeableEnchants = enchantRegistry.getOrThrow(EnchantmentTags.TRADEABLE);
					}
					for (Holder<Enchantment> e : tradeableEnchants) {
						String enchId = e.unwrapKey()
							.map(ResourceKey::identifier)
							.map(Identifier::toString)
							.orElse(null);
						if (enchId == null) continue;
						WantedTrade w = new WantedTrade(professionKey, itemId.toString(), enchId, 0);
						if (!entries.contains(w)) {
							entries.add(w);
							entryMaxLevels.add(e.value().getMaxLevel());
						}
					}
				} else {
					WantedTrade w = new WantedTrade(professionKey, itemId.toString(), null, 0);
					if (!entries.contains(w)) {
						entries.add(w);
						entryMaxLevels.add(1);
					}
				}
			}
		}
	}

	private static String prettyLabel(WantedTrade w) {
		if (w.getEnchantmentId() != null) {
			return prettyName(w.getEnchantmentId()) + " (Book)";
		}
		return prettyName(w.getItemId());
	}

	private String prettyProfessionName() {
		return prettyName(professionKey);
	}

	private static String prettyName(String id) {
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

	private class WishlistRowWidget extends AbstractWidget {
		private final WantedTrade entry;
		private final ItemStack iconStack;
		private final boolean marked;
		private final String label;
		private final int maxLevel;

		private WishlistRowWidget(int x, int y, int width, int height, WantedTrade entry,
				ItemStack iconStack, boolean marked, int maxLevel) {
			super(x, y, width, height, Component.literal(prettyLabel(entry)));
			this.entry = entry;
			this.iconStack = iconStack;
			this.marked = marked;
			this.label = prettyLabel(entry);
			this.maxLevel = maxLevel;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			int scissorX = TradeWishlistScreen.this.width / 2 - 125;
			int scissorY = HEADER_HEIGHT;
			int scissorRight = scissorX + LIST_WIDTH + 10;
			int scissorBottom = TradeWishlistScreen.this.height - FOOTER_HEIGHT;
			g.enableScissor(scissorX, scissorY, scissorRight, scissorBottom);

			Minecraft minecraft = Minecraft.getInstance();
			Font font = minecraft.font;
			if (isMouseOver(mouseX, mouseY)) {
				g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
			}
			if (marked) {
				g.text(font, Component.literal("\u2713"), getX() + 4, getY() + 5, 0xFFFFD700);
			} else {
				g.text(font, Component.literal("\u25CB"), getX() + 4, getY() + 5, 0xFF888888);
			}
			g.item(iconStack, getX() + 16, getY() + 2);
			g.itemDecorations(font, iconStack, getX() + 16, getY() + 2);
			g.text(font, Component.literal(label), getX() + 36, getY() + 6, 0xFFFFFFEE);

			if (maxLevel > 1) {
				int zoneStart = getWidth() - LEVEL_REGION_WIDTH;
				if (isMouseOver(mouseX, mouseY) && (mouseX - getX()) >= zoneStart) {
					g.fill(getX() + zoneStart, getY(), getX() + getWidth(), getY() + getHeight(), 0x60FFFFFF);
				}
				String levelText = "Lv: " + levelLabel(entry.getEnchantmentLevel());
				int textWidth = font.width(levelText);
				int levelX = getX() + getWidth() - textWidth - 6;
				g.text(font, Component.literal(levelText), levelX, getY() + 6, 0xFFAAFFAA);
			}

			g.disableScissor();
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
			if (event.button() != 0 || !isMouseOver(event.x(), event.y())) {
				return false;
			}
			int clickXInRow = (int) (event.x() - getX());
			int zoneStart = getWidth() - LEVEL_REGION_WIDTH;
			if (maxLevel > 1 && clickXInRow >= zoneStart) {
				int current = entry.getEnchantmentLevel();
				int newLevel = (current + 1) % (maxLevel + 1);
				chosenLevelByEnchant.put(entry.getEnchantmentId(), newLevel);
				if (marked) {
					TradeChoiceClient.getConfig().updateLevelFor(
							entry.getProfession(),
							entry.getItemId(),
							entry.getEnchantmentId(),
							newLevel);
				}
				playDownSound(Minecraft.getInstance().getSoundManager());
				TradeWishlistScreen.this.rebuildWidgets();
			} else {
				TradeChoiceClient.getConfig().toggle(entry);
				playDownSound(Minecraft.getInstance().getSoundManager());
				TradeWishlistScreen.this.rebuildWidgets();
			}
			return true;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}

		private static String levelLabel(int level) {
			if (level <= 0) return "Any";
			switch (level) {
				case 1: return "I";
				case 2: return "II";
				case 3: return "III";
				case 4: return "IV";
				case 5: return "V";
				case 6: return "VI";
				case 7: return "VII";
				case 8: return "VIII";
				default: return String.valueOf(level);
			}
		}
	}
}
