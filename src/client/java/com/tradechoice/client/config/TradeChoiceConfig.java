package com.tradechoice.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tradechoice.TradeChoiceMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradeChoiceConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("trade-choice");
	private static final Path CONFIG_FILE = CONFIG_DIR.resolve("choices.json");

	private final List<WantedTrade> choices = new ArrayList<>();

	public void load() {
		if (!Files.exists(CONFIG_FILE)) return;
		try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
			ConfigData data = GSON.fromJson(reader, ConfigData.class);
			if (data != null && data.choices != null) {
				choices.clear();
				choices.addAll(data.choices);
			}
		} catch (Exception e) {
			TradeChoiceMod.LOGGER.error("[trade-choice] Failed to load config", e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_DIR);
			ConfigData data = new ConfigData();
			data.choices = new ArrayList<>(choices);
			try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception e) {
			TradeChoiceMod.LOGGER.error("[trade-choice] Failed to save config", e);
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
		return choices.contains(trade);
	}

	public void toggle(WantedTrade trade) {
		if (choices.contains(trade)) {
			choices.remove(trade);
		} else {
			choices.add(trade);
		}
		save();
	}

	private static class ConfigData {
		List<WantedTrade> choices;
	}
}
