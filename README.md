<p align="center">
  <img src="docs/banner.png" alt="MCStore — stock-market economy and chest-GUI shop for Fabric, singleplayer and servers" width="100%">
</p>

<h1 align="center">MCStore</h1>

<p align="center">
  A stock-market economy and chest-GUI shop for Fabric that works in singleplayer worlds and on dedicated servers.<br>
  Buy and sell ores and minerals, watch stock and prices move, convert XP to money, and mine the Money Ore in the overworld and the nether.
</p>

<p align="center">
  <img alt="Minecraft 26.2" src="https://img.shields.io/badge/Minecraft-26.2-3c8527?style=flat-square">
  <img alt="Fabric" src="https://img.shields.io/badge/Fabric-client%20%2B%20server-dbb69c?style=flat-square">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-e76f00?style=flat-square">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square">
</p>

---

## A market, not a vending machine

MCStore is a small, focused economy for a Fabric world. Every tradable item has a **rarity-based stock cap**, the shop **restocks on a timer**, and prices move with supply just like a real market. Selling an item puts it back on the shelf; buying it takes it off the shelf. Players can also **exchange experience levels for money** and hold goods as an investment.

The shop is a normal chest interface, so it feels native and needs no custom menu screen. The only registry content is the **Money Ore**, a diamond-rarity ore that generates in both the overworld and the nether.

## Features

| | |
| --- | --- |
| **Chest-GUI shop** | `/buy` and `/sell` open a paged 9x6 chest. Every click is verified by the server. |
| **Rarity stock caps** | Common items have 512 units, uncommon 256, rare 64, epic 16, legendary 4. |
| **Restocking market** | Every 600 seconds the market returns to half stock, leaving room to buy and sell. The GUI and `/stock` show the countdown. |
| **Real supply and demand** | Buying removes stock and raises the live price; selling adds stock and lowers it. Prices move between -25% and +25% from the base price. |
| **Sell-to-restock** | A sold ore is immediately available for another player to buy. A completely full item stops buying. |
| **Money Ore** | A rare ore, similar to diamond, that generates in overworld stone/deepslate and in netherrack. |
| **XP exchange** | `/exchange` opens a chest GUI that converts experience levels to money at a configurable rate. |
| **Buy ores and minerals** | Ores, raw materials, ingots, nuggets, scraps and useful mineral blocks are buyable. |
| **Sell almost everything** | Every non-blacklisted vanilla item gets a generated sell price, including blocks, tools, food and drops. |
| **Sell categories** | The sell menu has separate Ores, Blocks, Minerals and All views. |
| **Sell chest** | `/sell chest` opens a deposit chest: sell one of each, a stack of each, the whole chest, your whole inventory or the held stack. |
| **Block pricing** | Mineral storage blocks are priced from their base material at **8.90×** instead of being treated as unrelated items. |
| **Persistent economy** | Balances are stored as integer cents in `balances.json`; stock and the restock clock live in `stocks.json`. |
| **Live admin editor** | `/mcstore edit` opens a draft editor with buy/sell switching, +/- buttons and Save & Reload. |
| **Immediate command edits** | `/mcstore setprice <item> <buy\|sell> <price>` writes the file and refreshes open shop views. |
| **Deterministic defaults** | The same item id always gets the same generated price, and prices stay inside their rarity band. |
| **Client + server** | One universal jar works in singleplayer and on servers. |

## Preview

<p align="center">
  <img src="docs/screenshots/buy.png" alt="Buy screen mockup" width="49%">
  <img src="docs/screenshots/sell.png" alt="Sell screen mockup" width="49%">
</p>

<p align="center">
  <img src="docs/screenshots/admin.png" alt="Price editor screen mockup" width="64%">
  <br>
  <sub>Illustrative interface previews. The real screens use Minecraft's vanilla chest interface with live stock bars and market prices.</sub>
</p>

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | **26.2** |
| Fabric Loader | **0.20.1** or newer |
| Fabric API | **0.161.0+26.2** or newer for 26.2 |
| Java | **25** |

MCStore is built for the 26.2 line. The mod metadata declares `minecraft: "~26.2"`, so a 26.3 game will refuse to load it until a matching build is published.

## Installation

### Singleplayer / client

1. Install **Fabric Loader** for Minecraft **26.2** using the official installer.
2. Put **Fabric API** and **MCStore** into `.minecraft/mods/` (or your launcher profile's `mods/` directory).
3. Start the game and open or create a world. MCStore creates `config/mcstore/` and registers `/buy`, `/sell`, `/exchange`, `/stock`, `/balance` and `/mcstore`.
4. Run `/buy` or `/sell` to open the shop, or `/exchange` to convert XP. In a survival world without cheats, those player commands still work; `/mcstore` needs cheats/op or a UUID in `admins.json`.

### Dedicated server

1. Install a Fabric server for Minecraft **26.2**. The official Fabric installer works:
   ```bash
   java -jar fabric-installer.jar server -mcversion 26.2 -loader 0.20.1 -downloadMinecraft
   ```
2. Put **Fabric API** and **MCStore** into the server's `mods/` directory.
3. Start the server. MCStore creates `config/mcstore/`, logs the command registration and generates the price book and stock clock.
4. Join as an operator (or add your UUID to `admins.json`) and run `/mcstore` to see the status.
5. Run `/buy`, `/sell`, `/exchange` or `/stock` to use the economy.

Install the same jar on the client for singleplayer worlds and to see the Money Ore texture and model. The jar is one universal build, not separate client and server downloads.

## Commands

| Command | Who | Description |
| --- | --- | --- |
| `/buy` | Player | Open the buy GUI. |
| `/buy <item>` | Player | Buy one of a buyable item directly, for example `/buy minecraft:diamond`. |
| `/sell` | Player | Open the sell GUI with Ores, Blocks, Minerals and All categories. |
| `/sell chest` | Player | Open the sell chest to deposit items and sell one, a stack, the chest or your inventory. |
| `/sell hand` | Player | Sell the stack you are holding. |
| `/sell all` | Player | Sell every sellable item in your inventory (subject to stock space). |
| `/exchange` | Player | Open the XP-to-money exchange GUI. |
| `/exchange <levels>` | Player | Convert that many experience levels to money. |
| `/exchange all` | Player | Convert every experience level you have. |
| `/stock <item>` | Player | Show stock, live buy/sell prices, trend and the restock countdown. |
| `/balance` | Player | Show your balance. |
| `/mcstore` | Admin | Show coverage, market status and restock time. |
| `/mcstore status` | Admin | Same as `/mcstore`. |
| `/mcstore stock <item>` | Admin | Same stock report as `/stock`. |
| `/mcstore restock` | Admin | Force an immediate full restock. |
| `/mcstore setprice <item> <buy\|sell> <price>` | Admin | Set one base price, write `prices.json` and refresh open views. |
| `/mcstore edit` | Admin, player | Open the live price editor. |
| `/mcstore reload` | Admin | Re-read `config.json`, `prices.json`, `stocks.json` and `admins.json`. |

Item arguments accept both `minecraft:diamond` and the short form `diamond`. Prices are decimal currency amounts with two decimals of precision, for example `55.00`.

### Permissions

- `/mcstore` and all of its subcommands require permission level **2** (GAMEMASTER), or a UUID listed in `config/mcstore/admins.json`.
- The server console always has access.
- `/buy`, `/sell`, `/exchange`, `/stock` and `/balance` are for players; the console receives a clear error for player-only actions.
- `/mcstore edit` needs a player because it opens a chest GUI.

## Using the shop

### The stock market

Every tradable item has a stock level and a cap that depends on rarity. A fresh
world and every restock use the **resting stock**, which is half of the cap:

| Rarity | Stock cap | Resting stock |
| --- | --- | --- |
| Common | 512 | 256 |
| Uncommon | 256 | 128 |
| Rare | 64 | 32 |
| Epic | 16 | 8 |
| Legendary | 4 | 2 |

The default cycle is **600 seconds** (`restockSeconds` in `config.json`). A restock sets every item back to its resting stock. That deliberately leaves stock to buy and room to sell; a completely full shelf would refuse every sale. `restockSeconds: 0` disables automatic restocks.

Prices are quoted from the base price in `prices.json` and a supply factor:

- **Half stock**: base price (stable).
- **Full stock**: base price × 0.75 (oversupplied).
- **Empty stock**: base price × 1.25 (scarce).
- The market is never off by more than ±25%, so prices stay readable and the base price remains an admin control.

Buying consumes stock, which pushes the next quote up. Selling adds stock, which pushes the next quote down. If an item is at its cap, the store stops buying it until stock is consumed or the next restock returns it to the resting level. If an item has no stock left, buying is refused until it is sold back or restocked.

This makes holding goods a real investment: buy while the shelves are full and cheap, then sell when the market is scarce and pays more.

### Buying

- Click an item to buy **one**.
- Shift-click to buy a full stack of that item.
- The purchase is checked in this order: price exists, stock is available, balance is sufficient, inventory has room. Money is only withdrawn after those checks pass, and a failed inventory insert is refunded and the stock is returned.
- The lore shows the live market buy price, a stock bar, the trend and the restock timer.
- The GUI displays the current balance on the gold ingot in the footer.

### Selling

- Click an item to sell **one**.
- Shift-click to sell **all of that item** currently in your inventory, limited by remaining stock space.
- The footer filters the list into **Ores**, **Blocks**, **Minerals** and **All**. The selected category is highlighted.
- **Open sell chest** (or `/sell chest`) opens a deposit chest. Put any block, ore, tool or other item in the top 45 slots, then choose:
  - **Sell 1 of each** - one unit from every item type in the chest.
  - **Sell a stack of each** - up to one stack of every item type in the chest.
  - **Sell everything in the chest** - every sellable deposited item.
  - **Sell entire inventory** - every sellable item you carry.
  - **Sell held item** and **Return chest items** are also available.
- Items that cannot be sold stay in the chest. Closing or leaving returns the remaining chest contents to your inventory (or drops them at your feet if it is full).
- `/sell hand` sells the held stack. `/sell all` walks the inventory and sells every item that has a price and stock space.
- Sale proceeds are credited immediately and the new balance is written to disk.

### Exchange

- `/exchange` opens a chest with offers for 1, 5, 10, 25 and all experience levels.
- `/exchange <levels>` and `/exchange all` convert directly from chat.
- The default rate is **1.00 per level** (`xpCentsPerLevel` in `config.json`).
- Levels are removed before the money is credited; the exchange cannot create money without spending XP.

### Money Ore

- The Money Ore is a **Rare** commodity, the same tier as diamond.
- Base market values: **sell 50.00**, **buy 150.00** (before the live supply factor).
- It generates in the overworld between bedrock and y=80, replacing stone and deepslate, and in the nether between y=10 and the top of the nether ceiling area, replacing netherrack.
- It drops itself, works with Silk Touch, needs an iron pickaxe (or better) and awards 2–5 experience when mined.
- Every generated ore can be sold to the store, and every sale adds to the stock another player can buy.
- Admin-editable like any other entry: `/mcstore setprice mcstore:money_ore sell 60.00`.

### Paging and navigation

The shop shows 45 entries per page. The bottom row holds the page arrows, balance, category filters, sell-chest shortcut and close button. Display items cannot be removed from the buy/sell lists: all clicks are intercepted by the server menu. The sell chest is the only screen that accepts item movement, and it only accepts items into its 45 deposit slots.

## Prices and rarity

Mineral storage blocks (for example `iron_block`, `raw_iron_block`, `diamond_block` and `netherite_block`) are priced from the material they are crafted from at **8.90×** the base price. A block is nine materials in the crafting grid, so 8.90× keeps a block slightly cheaper than nine separate units while making blocks worth selling. The multiplier applies to both the sell and buy price and uses the material's rarity.

An existing `prices.json` is migrated once: mineral block entries are replaced with the 8.90× price and the file receives a `_version` marker. After that, `/mcstore setprice` and the editor keep your block prices untouched.

MCStore uses five rarity bands. Default sell prices are deterministic and stay inside the band. Generated buy prices are between **2x and 4x** the sell price; the multiplier rises with rarity.

| Rarity | Sell band | Default buy multiplier | Stock cap |
| --- | --- | --- | --- |
| Common | 1.00 – 5.00 | 2.0x | 512 |
| Uncommon | 5.00 – 15.00 | 2.5x | 256 |
| Rare | 15.00 – 40.00 | 3.0x | 64 |
| Epic | 40.00 – 100.00 | 3.5x | 16 |
| Legendary | 100.00 – 250.00 | 4.0x | 4 |

On a clean Minecraft 26.2 server the generator produced:

- **1,421 vanilla prices**
- **1,421 of 1,421 filtered items (100%)**
- **1,421 of 1,537 registry items (92%)**
- **57 buyable ore/mineral entries**, including the Money Ore
- **1,422 total price entries** (the 1,421 vanilla entries plus the Money Ore)

Coverage is measured against the filtered set: every `minecraft:` item that is not intentionally blacklisted. Technical and creative-only items, spawn eggs, command and structure blocks, portals, bedrock, barriers, light blocks, budding amethyst and similar entries are excluded.

Generated prices are written once to `prices.json`. Existing files are loaded as-is on restart, so admin edits are not overwritten. If the file is empty or missing, defaults are generated again. The Money Ore entry is added to an older price file only when it is missing.

## Configuration

All files live in `config/mcstore/` relative to the server directory.

### `config.json`

```json
{
  "_comment": "MCStore server configuration. startingBalance is a decimal currency amount (for example 100.00); refreshOpenGuis updates open views instantly; restockSeconds is the market cycle; xpCentsPerLevel is the value of one experience level.",
  "startingBalance": 100.00,
  "refreshOpenGuis": true,
  "restockSeconds": 600,
  "xpCentsPerLevel": 1.00
}
```

- `startingBalance` is used the first time a player's balance is requested. Changing it does **not** top up players who already have a ledger entry.
- `refreshOpenGuis: true` redraws open shop views after a price change. Set it to `false` if you prefer to close open shops when prices change.
- `restockSeconds` is the full-restock cycle. `0` disables automatic restocking.
- `xpCentsPerLevel` is the exchange rate for one experience level.

### `prices.json`

```json
{
  "items": {
    "minecraft:diamond": {
      "rarity": "RARE",
      "buy": 120.00,
      "sell": 40.00
    },
    "mcstore:money_ore": {
      "rarity": "RARE",
      "buy": 150.00,
      "sell": 50.00
    },
    "minecraft:oak_log": {
      "rarity": "COMMON",
      "sell": 2.18
    }
  }
}
```

- `sell` is required for an item to be sellable.
- `buy` is optional. Omit it (or set it to `null`) to make an item **sell-only**.
- `rarity` is `COMMON`, `UNCOMMON`, `RARE`, `EPIC` or `LEGENDARY`. Case-insensitive names are accepted.
- Prices may be JSON numbers or decimal strings; both are converted to integer cents.
- These are **base** prices. The live price is calculated from stock at query time.
- After hand-editing, run `/mcstore reload` (or restart) so the server picks up the file.

### `stocks.json`

```json
{
  "_comment": "MCStore stock. An item that is not listed is at its resting stock (half of its cap).",
  "restockSeconds": 600,
  "lastRestock": 1792741200,
  "stocks": {
    "minecraft:diamond": 23,
    "mcstore:money_ore": 7
  }
}
```

- Items that are not listed are at their resting stock (half of the cap). Only items that have been traded are written here.
- `lastRestock` is Unix epoch seconds and drives the countdown.
- `stocks.json` is rewritten atomically after every trade, restock and shutdown.
- To reset the market, replace the file with `{}` (or delete it) while the server is stopped, then start the server.

### `admins.json`

```json
{
  "admins": [
    "069a79f4-44e9-4726-a5be-fca90e38aaf5"
  ]
}
```

Op level 2 already has access; this file adds extra UUIDs. The file also accepts a bare array. Run `/mcstore reload` after editing it.

### `balances.json`

```json
{
  "balances": {
    "069a79f4-44e9-4726-a5be-fca90e38aaf5": 10000
  }
}
```

Values are **integer cents** (`10000` = `100.00`). MCStore writes this file on every balance change and again on server shutdown. For a manual balance adjustment, stop the server first, edit the file, then start it again.

## Admin workflow

### Live editor

`/mcstore edit` opens a draft of the whole price book.

- Click the comparator to switch between editing **BUY** and **SELL** prices.
- Click an item with left mouse to add **1.00**; right mouse subtracts **1.00**.
- Hold shift to change the value by **10.00** per click.
- **Save & Reload** writes `prices.json` immediately and refreshes open `/buy` and `/sell` views.
- The editor shows base prices; the live price still moves with stock.
- The hopper discards the draft and reloads the file from disk.
- The barrier closes the editor.

The draft is held in memory until you save, so you can adjust several prices before committing.

### Command edit

```mcfunction
mcstore setprice minecraft:diamond sell 55.00
mcstore setprice minecraft:diamond buy 165.00
mcstore setprice mcstore:money_ore sell 60.00
mcstore restock
mcstore reload
```

`setprice` writes immediately. Buy prices are only accepted for items in the buyable ore/mineral set; sell prices can be set for any registered item that already has a price entry.

## Persistence and backups

- Prices, balances, stock and config are plain JSON. They are deliberately readable and easy to diff.
- `balances.json`, `prices.json` and `stocks.json` are written atomically; a crash should not leave a half-written file.
- Back up `config/mcstore/` before changing prices, resetting an economy or moving a world.
- Removing the mod removes its commands, the shop GUI and the Money Ore registry content. The JSON files remain and can be reused if the mod is installed again.

## Building from source

Requirements: JDK **25** and network access to the Fabric Maven repository.

```bash
./gradlew test
./gradlew build
```

The build produces `build/libs/MCStore-26.2.4.jar` (and a lowercase copy, `build/libs/mcstore-26.2.4.jar`). The jar is the only build output that should be published. It is a universal client + server mod (`environment: "*"`), so the same file goes into a client `mods/` folder or a server `mods/` folder.

### Tests

The repository contains unit tests for rarity bands, price generation, block pricing, sell categories, blacklist and buyable classification, coverage math, JSON price persistence, stock persistence, market quotes, balance persistence, config parsing and the item-id command argument.

### Smoke test

`scripts/smoke-test.sh` builds the mod, downloads a Fabric 26.2 server and Fabric API into `build/smoke/` (ignored by Git), starts the server, runs commands from the console, stops it and verifies the log, generated files, and generated worlds:

```bash
JAVA_HOME=/path/to/jdk-25 scripts/smoke-test.sh
```

The script checks that the mod initializes, the commands register, the 80% coverage target passes, stock and `/mcstore restock` work, the Money Ore block can be placed and generates in both the overworld and the nether, `/mcstore setprice` writes the new price, `/mcstore reload` works and `config.json`, `prices.json`, `balances.json`, `stocks.json` and `admins.json` are created.

## Compatibility notes

- **Minecraft 26.2** only in this release.
- **Client + server**: one universal jar. Install it on both sides for singleplayer, the full Money Ore experience and the exchange GUI.
- **Other server-side mods** are generally fine. If another mod already registers `/buy`, `/sell`, `/exchange`, `/stock`, `/balance` or `/mcstore`, the command trees may conflict; remove or rename one of them.
- The mod is not affiliated with Mojang Studios or Microsoft.

## FAQ

**Can a player take the display items out of the shop?**  
No. Every click is handled by the server menu, item movement is disabled, and the shop container is server-owned.

**Can a player buy with a full inventory?**  
The purchase is refused before money is withdrawn. If the final inventory insert unexpectedly fails, the cost is refunded and the stock is returned.

**Why did an item sell for less than the base price?**  
Because the store was already well stocked. The live sell price drops as stock rises and rises when the shelves are empty.

**Why can I not sell this item?**  
The store is at its stock cap for that item and is not buying more until stock is consumed or the next restock happens.

**Why can I not buy this item?**  
The shelf is empty. Wait for stock to be sold back or for the next restock.

**Where do I change prices?**  
Use `/mcstore edit`, `/mcstore setprice`, or edit `config/mcstore/prices.json` and run `/mcstore reload`.

**Where do I change the restock timer or the XP rate?**  
Edit `restockSeconds` or `xpCentsPerLevel` in `config/mcstore/config.json` and run `/mcstore reload`.

**Why is an item missing from the shop?**  
It is either blacklisted as technical/creative-only, or there is no price entry for it. `/mcstore setprice` reports unknown items clearly.

**How do I reset the economy?**  
Stop the server, back up and replace or empty `config/mcstore/balances.json`, then start the server. New players receive `startingBalance`. For a market reset, replace `stocks.json` with `{}`.

**Does it work on 26.3?**  
Not in this release. The mod metadata targets `~26.2`; a 26.3 build requires a new release.

## License

MIT. See [LICENSE](LICENSE).
