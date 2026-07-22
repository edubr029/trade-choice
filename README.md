<a href="https://modrinth.com/mod/trade-choice"><img src="https://cdn.modrinth.com/data/jqSyrAQy/e77565163615fa20c36f1654bad96260d123582a.png" width="150" align="left"></a>

# Trade Choice

Trade Choice is a Minecraft mod that watches villager trade screens for you. Pin the trades you want, and an audio alert plays the moment a villager offers them. Pair it with [Trade Cycling](https://modrinth.com/mod/trade-cycling) for hands-free auto-search — the mod cycles the villager on your behalf until a wanted trade appears, then alerts you and stops.

## Downloads

- **Modrinth** — https://modrinth.com/mod/trade-choice ***(recommended!)***
- **GitHub Releases** — https://github.com/edubr029/trade-choice/releases

The same JAR works on both Fabric and NeoForge — no separate downloads.

## Features

- **Wishlist UI** — dedicated screen for managing the trades you want, keyed per villager profession (e.g., separate marks for librarian books, armorer tools, etc.)
- **Audio alert** — a chime plays the moment a villager offers one of your marked trades
- **Auto-search with Trade Cycling** — the mod cycles the villager's offers automatically until a wanted trade appears, then alerts you and stops
- **Shift-click to mark** — toggle a "wanted" mark from anywhere on the trade screen, no extra keybind to remember
- **Client-side only**

## How to use

**Mark a wanted trade**
1. Open a villager's trade screen
2. **Shift-click** any trade slot to toggle it as wanted for that villager's profession

**View your wishlist**
1. Open a villager's trade screen
2. Click the **Wanted** button (magnifying glass icon, next to Auto Search)

**Run auto-search**
1. Open a villager's trade screen (must be below master level — buttons are hidden at master level)
2. Click **Auto Search** — the mod cycles the villager's offers until one of your marked trades appears
3. An audio alert plays and cycling stops on the matching offer

**Stop a running auto-search**
- Click **Stop Search** on the same screen, or
- Close the trade screen

## Companion mod: Trade Cycling

Auto-search is powered by [Trade Cycling](https://modrinth.com/mod/trade-cycling) by henkelmax — a popular mod that adds the trade-reroll button to the villager screen. **Trade Choice works standalone** for wishlist marking and audio alerts; auto-search requires Trade Cycling to be installed alongside.

## Configuration

Wanted trade marks persist per-world in a JSON file at `<minecraft>/config/tradechoice/choices.json`. The file is auto-created on first use and safe to edit manually — the mod reloads it on the next screen open.

## Supported platforms

| Minecraft | Fabric | NeoForge |
|---|:---:|:---:|
| **26.2** | ✅ | ✅ |

Java 25 is required on all loaders. On Fabric, [Fabric API](https://modrinth.com/mod/fabric-api) is also required (auto-installed by most launchers).

## Reporting issues

Found a bug or have a feature idea? Open an issue on the [GitHub issue tracker](https://github.com/edubr029/trade-choice/issues).

## License

[MIT](./LICENSE) — © [edubr029](https://github.com/edubr029)
