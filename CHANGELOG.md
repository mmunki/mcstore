# Changelog

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
