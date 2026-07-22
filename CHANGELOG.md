# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
