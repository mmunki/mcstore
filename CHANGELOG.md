# Changelog

## 26.2.2 — 2026-10-23

### Added

- Rarity stock caps: COMMON 512, UNCOMMON 256, RARE 64, EPIC 16, LEGENDARY 4.
- A live market quote for every item. Prices move between -25% and +25% of the
  base price as stock empties and refills.
- Automatic full restock every 600 seconds, configurable with `restockSeconds`
  (`0` disables it), persisted together with stock in `config/mcstore/stocks.json`.
- Selling adds the sold items to the store stock; a full item stops buying until
  stock is consumed or the market restocks. Buying consumes stock.
- `/stock <item>` and `/mcstore stock <item>` with stock, fill bar, market
  buy/sell prices, trend and restock countdown.
- `/mcstore restock` for an immediate full market restock.
- `/exchange`, `/exchange <levels>` and `/exchange all`, plus an XP Exchange
  chest GUI with 1, 5, 10, 25 and all-level offers. The default rate is 1.00
  per level and is configurable with `xpCentsPerLevel`.
- The Money Ore: a RARE block/item with diamond-tier value (base sell 50.00,
  buy 150.00) that generates in overworld stone/deepslate and in netherrack,
  drops itself, works with Silk Touch, requires an iron pickaxe and awards
  experience when mined.
- Block/item registration, blockstate/model/texture/language resources, loot
  table, pickaxe/iron-tier tags and data-driven overworld + nether worldgen.
- The shop GUI now shows live market prices, a stock bar, the market trend and
  the restock timer, and links directly to the XP exchange.

### Changed

- `prices.json` entries are now base prices; the server computes the live price
  from stock. Admin edits still work exactly as before.
- `/mcstore reload` now also reloads the stock clock.
- The status line reports the current restock countdown.
- The jar version is 26.2.2.

### Verified

- Unit tests: 46 passing.
- Fabric 26.2 dedicated-server smoke test on Linux x86_64, Java 25:
  - Fabric Loader 0.20.1, Fabric API 0.161.0+26.2
  - mod initialized and all six command roots registered
  - 1,422 price entries (1,421 vanilla + Money Ore), 100% filtered coverage
  - stock report, forced restock and `stocks.json` verified
  - Money Ore block placed with `/setblock`
  - Money Ore found in generated overworld and nether region files
  - `/mcstore setprice` and `/mcstore reload` verified
- Release JAR is published as an asset of the `mcstore-26.2.2` release.

## 26.2.1 — 2026-10-23

### Fixed

- The jar now declares `environment: "*"` and a common `main` entrypoint instead of a
  server-only entrypoint. Fabric Loader no longer skips it on a client, so MCStore loads
  in singleplayer worlds (the integrated server) as well as on dedicated servers.
- `/buy`, `/sell`, `/balance` and `/mcstore` are now registered in singleplayer, which also
  restores their slash-command and item-id tab completion there. Previously the mod was
  never initialized on a client, so no commands existed to suggest.
- The price book and balances are reloaded per world session, so entering a world after
  leaving another one restores the store correctly.

### Changed

- One universal client + server jar. The same `MCStore-26.2.1.jar` goes into a client
  `mods/` folder or a server `mods/` folder; there is no modpack.
- Added a metadata unit test and a smoke-test check that fail if the jar ever becomes
  server-only again.

## 26.2 — 2026-10-23

Initial release for Minecraft 26.2.

### Added

- Server-only Fabric mod with no client entrypoint and no registry content.
- `/buy` and `/sell` chest GUIs with 45 entries per page, balance display and safe pagination.
- Direct `/buy <item>`, `/sell hand` and `/sell all` commands.
- Persistent integer-cent balances in `config/mcstore/balances.json`, starting at `100.00`.
- Generated price book covering every non-blacklisted vanilla item, with five rarity bands and 2x–4x buy multipliers.
- `/balance` and admin status reporting with an explicit 80% coverage target.
- `/mcstore setprice <item> <buy|sell> <price>` with immediate JSON write and open-view refresh.
- `/mcstore edit` chest editor with buy/sell switching, +/- controls, draft state, Save & Reload and reload-from-disk.
- `/mcstore reload` for `config.json`, `prices.json` and `admins.json`.
- `admins.json` extra-admin UUID list, with op level 2 always allowed.
- Atomic JSON writes and a shutdown balance save.

### Verified

- Unit tests: 30 passing.
- Fabric 26.2 dedicated-server smoke test on Linux x86_64, Java 25:
  - Fabric Loader 0.20.1
  - Fabric API 0.161.0+26.2
  - mod initialized and all four commands registered
  - 1,421 generated prices, 1,421 of 1,421 filtered items (100%), 1,421 of 1,537 registry items (92%)
  - 56 buyable ore/mineral entries
  - `/mcstore setprice minecraft:diamond sell 55.00` wrote `prices.json`
  - `/mcstore setprice diamond buy 123.00` accepted the short item id and wrote the buy price
  - `/mcstore reload` re-read the file successfully
  - `config.json`, `prices.json`, `balances.json` and `admins.json` created on first start
- Release JAR is 140,234 bytes (about 139 KB), well below the 1 MB target.

### Notes

- Targets Minecraft `~26.2`; a 26.3 build is a separate release.
- Vanilla clients need no mod; the shop uses the vanilla chest menu.
