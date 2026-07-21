package com.tradechoice.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tradechoice.TradeChoiceMod;
import com.tradechoice.client.platform.Platforms;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TradeChoiceConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configDir = Platforms.get().getConfigDir().resolve("tradechoice");
	private final Path configFile = configDir.resolve("choices.json");

	private final List<WantedTrade> choices = new ArrayList<>();

	public void load() {
		if (!Files.exists(configFile)) return;
		try (Reader reader = Files.newBufferedReader(configFile)) {
			ConfigData data = GSON.fromJson(reader, ConfigData.class);
			if (data != null && data.choices != null) {
				choices.clear();
				choices.addAll(data.choices);
			}
		} catch (Exception e) {
			TradeChoiceMod.LOGGER.error("[tradechoice] Failed to load config", e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(configDir);
			ConfigData data = new ConfigData();
			data.choices = new ArrayList<>(choices);
			try (Writer writer = Files.newBufferedWriter(configFile)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception e) {
			TradeChoiceMod.LOGGER.error("[tradechoice] Failed to save config", e);
		}
	}

	public List<WantedTrade> getChoices() {
		return Collections.unmodifiableList(choices);
	}

	public List<WantedTrade> getChoicesForProfession(String profession) {
		return choices.stream()
				.filter(c -> c.matchesProfession(profession))
				.toList();
	}

	public boolean isMarked(WantedTrade trade) {
		return choices.stream().anyMatch(c -> wantedMatches(c, trade));
	}

	/**
	 * Updates the level of the saved trade matching (profession, itemId, enchantmentId).
	 * Returns false if no saved trade was found, or if the level is unchanged.
	 */
	public boolean updateLevelFor(String profession, String itemId, String enchantmentId, int newLevel) {
		WantedTrade existing = choices.stream()
				.filter(c -> Objects.equals(c.getProfession(), profession)
						&& Objects.equals(c.getItemId(), itemId)
						&& Objects.equals(c.getEnchantmentId(), enchantmentId))
				.findFirst().orElse(null);
		if (existing == null || existing.getEnchantmentLevel() == newLevel) {
			return false;
		}
		choices.remove(existing);
		choices.add(new WantedTrade(profession, itemId, enchantmentId, newLevel));
		save();
		return true;
	}

	private static boolean wantedMatches(WantedTrade saved, WantedTrade probe) {
		if (!Objects.equals(saved.getProfession(), probe.getProfession())) return false;
		if (!Objects.equals(saved.getItemId(), probe.getItemId())) return false;
		if (!Objects.equals(saved.getEnchantmentId(), probe.getEnchantmentId())) return false;
		int savedLvl = saved.getEnchantmentLevel();
		int probeLvl = probe.getEnchantmentLevel();
		return savedLvl == 0 || probeLvl == 0 || savedLvl == probeLvl;
	}

	public void toggle(WantedTrade trade) {
		if (choices.contains(trade)) {
			choices.remove(trade);
		} else {
			List<WantedTrade> existing = choices.stream()
					.filter(c -> c.matchesProfession(trade.getProfession()))
					.toList();
			if (!existing.isEmpty()) {
				TradeChoiceMod.LOGGER.info(
						"[tradechoice] Replacing {} existing mark(s) for profession={} with new mark on item={}",
						existing.size(), trade.getProfession(), trade.getItemId());
				choices.removeAll(existing);
			}
			choices.add(trade);
		}
		save();
	}

	public void clearForProfession(String profession) {
		choices.removeIf(c -> c.matchesProfession(profession));
		save();
	}

	private static class ConfigData {
		List<WantedTrade> choices;
	}
}
