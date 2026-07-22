# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-07-22

### Added
- Bundled vanilla 26.2 trade snapshot inside the JAR (`assets/tradechoice/fallback/vanilla_trades_26.2.json`) — covers all 13 villager professions across 5 levels + 3 wandering-trader profiles (68 trade sets) and the 40 enchantments in vanilla's `tradeable` enchantment tag, each annotated with `max_level`.
- New `VanillaTradeSnapshot` loader (lazy singleton, double-checked locking) for accessing the bundled snapshot data via the classpath resource.
- Three-tier fallback resolution in `TradeWishlistScreen.buildEntries()`: canonical TRADE_SET registry lookup → bundled vanilla snapshot → live `MerchantMenu` offers as last resort.

### Fixed
- Multiplayer crash on dedicated servers that don't sync the TRADE_SET dynamic registry to clients (e.g., `non-vanilla dedicated server`). The Wanted trade panel previously threw `IllegalStateException: Missing registry: ResourceKey[minecraft:root / minecraft:trade_set]` on non-vanilla servers; it now degrades gracefully through the bundled snapshot.
- Tier-3 `MerchantMenu` fallback deduplication: multiple live offers of the same enchantment at different levels (e.g., Efficiency I / II / III) no longer render as duplicate wishlist rows — enchant levels collapse to `0` for `WantedTrade` equality, matching the canonical and snapshot paths.
- Tier-3 `MerchantMenu` fallback enchantment picker: `entryMaxLevels` now look up the enchantment's real maximum level from the bundled snapshot instead of being hardcoded to `1`, so the level picker on enchantment rows renders the correct range.

## [1.0.0] - 2026-07-21

### Added
- Initial release
- Single universal JAR works on both Fabric and NeoForge for Minecraft 26.2
- Custom `mergeJar` Gradle task for producing the universal JAR (no external dependencies)
- Wishlist UI screen for marking desired villager trades
- Audio alert when a villager offer matches a marked trade
- Auto-search mode that cycles villager trades via the [Trade Cycling](https://modrinth.com/mod/trade-cycling) mod until a marked offer appears (optional — requires Trade Cycling installed alongside)
- Shift-click a villager trade to toggle a persistent "wanted" mark keyed by the villager's profession (e.g., one mark for librarian books, separate marks for armorer tools, etc.)
- 128×128 mod icon displayed in mod lists on both Fabric Loader (via Mod Menu) and NeoForge (in-game Mods screen)
- Author metadata set to `edubr029`
- Project URLs wired into both loader manifests: Modrinth homepage (`https://modrinth.com/mod/trade-choice`) and GitHub sources (`https://github.com/edubr029/trade-choice`)
- Optional dependency on Trade Cycling declared per-loader (`suggests` in `fabric.mod.json`, `optional` dep in `neoforge.mods.toml`)
- Platform abstraction (`FabricPlatform` / `NeoForgePlatform`) that hides loader-specific API differences (loader detection, config dir, payload dispatch) from shared common code
- Reflective lookup of the Trade Cycling packet class — works unchanged on both loaders since the class name is identical across Fabric and NeoForge builds
- Auto Search and Wanted buttons are hidden when trading with a master-level villager (auto-search is functionally useless when the villager cannot level up anymore)
