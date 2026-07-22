package com.tradechoice.client.fallback;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.tradechoice.TradeChoiceMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaTradeSnapshot {

    private static final String RESOURCE_PATH = "/assets/tradechoice/fallback/vanilla_trades_26.2.json";
    private static final Gson GSON = new Gson();

    private static volatile Loaded snapshot;

    private VanillaTradeSnapshot() {
    }

    public static final class EnchantMeta {
        private final String id;
        private final int maxLevel;

        EnchantMeta(String id, int maxLevel) {
            this.id = id;
            this.maxLevel = maxLevel;
        }

        public String id() {
            return id;
        }

        public int maxLevel() {
            return maxLevel;
        }
    }

    public static final class ItemEntry {
        private final String itemId;

        ItemEntry(String itemId) {
            this.itemId = itemId;
        }

        public String itemId() {
            return itemId;
        }
    }

    private static final class Loaded {
        final List<EnchantMeta> tradeableEnchants;
        final Map<String, List<ItemEntry>> tradeSets;
        final boolean present;

        Loaded(List<EnchantMeta> tradeableEnchants, Map<String, List<ItemEntry>> tradeSets, boolean present) {
            this.tradeableEnchants = tradeableEnchants;
            this.tradeSets = tradeSets;
            this.present = present;
        }
    }

    private static final class RawEnchant {
        @SerializedName("id") String id;
        @SerializedName("max_level") int maxLevel;
    }

    private static final class RawItem {
        @SerializedName("item") String item;
    }

    private static final class Raw {
        @SerializedName("tradeable_enchants") List<RawEnchant> tradeableEnchants;
        @SerializedName("trade_sets") Map<String, List<RawItem>> tradeSets;
    }

    private static Loaded load() {
        Loaded existing = snapshot;
        if (existing != null) return existing;
        synchronized (VanillaTradeSnapshot.class) {
            if (snapshot != null) return snapshot;
            List<EnchantMeta> enchants = new ArrayList<>();
            Map<String, List<ItemEntry>> tradeSets = new HashMap<>();
            boolean present = false;
            try (InputStream in = VanillaTradeSnapshot.class.getResourceAsStream(RESOURCE_PATH)) {
                if (in == null) {
                    TradeChoiceMod.LOGGER.warn("[tradechoice] vanilla trade snapshot missing at {} (jar build regression?)", RESOURCE_PATH);
                } else {
                    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        Raw raw = GSON.fromJson(reader, Raw.class);
                        if (raw != null) {
                            if (raw.tradeableEnchants != null) {
                                for (RawEnchant re : raw.tradeableEnchants) {
                                    if (re != null && re.id != null) {
                                        enchants.add(new EnchantMeta(re.id, re.maxLevel));
                                    }
                                }
                            }
                            if (raw.tradeSets != null) {
                                for (Map.Entry<String, List<RawItem>> e : raw.tradeSets.entrySet()) {
                                    List<ItemEntry> items = new ArrayList<>();
                                    if (e.getValue() != null) {
                                        for (RawItem ri : e.getValue()) {
                                            if (ri != null && ri.item != null) {
                                                items.add(new ItemEntry(ri.item));
                                            }
                                        }
                                    }
                                    tradeSets.put(e.getKey(), items);
                                }
                            }
                            present = !enchants.isEmpty() || !tradeSets.isEmpty();
                        }
                    }
                }
            } catch (Exception e) {
                TradeChoiceMod.LOGGER.warn("[tradechoice] failed to load vanilla trade snapshot", e);
            }
            Loaded result = new Loaded(enchants, tradeSets, present);
            snapshot = result;
            return result;
        }
    }

    public static List<ItemEntry> getTradesForSet(String tradeSetKey) {
        Loaded data = load();
        List<ItemEntry> list = data.tradeSets.get(tradeSetKey);
        return list == null ? Collections.emptyList() : list;
    }

    public static List<EnchantMeta> getTradeableEnchants() {
        return load().tradeableEnchants;
    }

    public static boolean isAvailable() {
        return load().present;
    }
}
