# Grounded: a NeoForge survival pack (staged build)

**Main target: Minecraft 26.2, NeoForge 26.2.0.93, Java 25.** It's the newest version that still has almost the whole mod list.
**Fallback: Minecraft 1.21.1, NeoForge 21.1.266, Java 21.** It has more mods, all stable releases.

Every jar was resolved from Modrinth (exact version, SHA-512 hash, required dependencies). To rebuild with current versions:

```
python build_pack.py          # 26.2  -> dist/26.2/
python build_pack.py 1.21.1   # fallback -> dist/1.21.1/
```

Import each `.mrpack` as its own instance in **Prism Launcher** or the **Modrinth App**. Give it 6 GB of memory. The launcher picks Java 25 for 26.2 automatically.

## Why 26.2 and not 26.3

I checked every mod across all releases from 1.21.5 to 26.3.
- **26.3** loses Cold Sweat, Attract to Sound and more.
- **26.2** is the newest version where the core of the pack survives.

| Role | 26.2 (main) | 1.21.1 (fallback) |
|---|---|---|
| Medical | Medical System 2.14.0 | Medical System 2.14.2 |
| Temperature | Cold Sweat 2.4.5-**a2 (alpha)** | Cold Sweat 2.4.5 |
| Thirst | **Thirst Was Taken 2** (supports Cold Sweat and Serene Seasons) | Thirst Was Taken 2.1.5 (Cold Sweat integration off) |
| Seasons | Serene Seasons (beta) + GlitchCore | Serene Seasons (beta) |
| Cooking | **Crop & Kettle** (Farmer's Delight has no 26.x build) | Farmer's Delight |
| Food variety | Spice of Life: Onion | Spice of Life: Onion |
| Mob AI | Attract to Sound | Attract to Sound **or** Hostile Tactics |
| Spawns | In Control (beta) | In Control (beta) |
| Performance | Sodium, FerriteCore, Entity Culling, ImmediatelyFast, Distant Horizons | the same + ModernFix |
| Audio | Sound Physics Remastered, AmbientSounds | the same |
| Utility | JEI, AppleSkin | JEI, AppleSkin |

## 26.2 files (`dist/26.2/`)

| File | Contents |
|---|---|
| `grounded-1-medical.mrpack` | Medical System, JEI, AppleSkin, performance mods |
| `grounded-2-survival.mrpack` | + Cold Sweat, Thirst Was Taken 2, Serene Seasons, Crop & Kettle, Spice of Life: Onion |
| `grounded-3-sound-ai.mrpack` | + Attract to Sound, In Control with `spawn.json` |

`dist/1.21.1/` still contains the earlier tested four-stage build (3a sound AI and 3b tactics AI).

## Design decisions (both versions)

**Medical: Medical System (TarkovCraft), not First Aid New.** Running both would conflict.
- 7 limbs, each with its own health. Losing the head or torso is fatal. Armour only protects the limb it covers.
- Bleeds, fractures, pain and blood loss.
- Items: Bandage, Pressure Dressing, Tourniquet and Splint (arms and legs only), Painkillers, Morphine, Blood Bag, First Aid Kit, Regenerative Injector, and a 15-second Emergency Surgery Kit.
- Downed players can be revived by a teammate.

**Animations (checked in the source code):** neither medical mod has Tarkov-style first-person hand animations.
- Medical System uses vanilla hold-to-use poses (bow-draw or eating). Treatment is timed, slows you down, and heals in cycles, so letting go early gives only partial healing.
- First Aid New has no hand animation at all.
- Real animations would need a small custom add-on mod.

**Spawns (`overrides/incontrol/config/incontrol/spawn.json`):**
- Mob spawners are left untouched.
- Overworld hostiles are capped at 30.
- 40% of natural overworld hostile spawns are denied.
- Bandits and survivors have their own limits; see *Human spawn limits* below.

**Uncertainty (26.2):** the pack deliberately hides information.
- **No AppleSkin**, so you don't see exact food or saturation values.
- **True Darkness by Tia:** light level 0 is pitch black, and gamma can't get around it. Light sources work as normal.
- **Dungeons and Taverns:** 212 structures (bunkers, catacombs, camps and more), so there's loot worth an expedition.
- **Hidden coordinates:** this is a per-world setting, so the pack can't set it for you. When creating a world, go to *More → Game Rules* and turn on **Reduced Debug Info** (`reduced_debug_info`), which hides coordinates in F3. In an existing world, use `/gamerule reduced_debug_info true`, which needs cheats. Leave it off while you're testing.
- **JEI** stays in for now, for testing. For a real playthrough, delete the JEI jar so you discover recipes yourself.

**Movement (26.2):**
- **Grounded Tweaks** (`overrides/tweaks/mods/grounded-tweaks-1.0.0.jar`, source in `tweaks-src/`) is a code-free mod with a built-in datapack. Once per second it gives every player a **−15% base movement speed** modifier (`grounded:slower_walk`), which lowers walking, sprinting and sneaking alike. It re-applies after respawn and never stacks. Tested on 26.2: speed 0.23 → 0.1955 on a test mob.
  - To tune it, edit `-0.15` in `tweaks-src/data/grounded/function/loop.mcfunction` and rebuild the jar.
  - To turn it off in a world, run `/attribute @s minecraft:movement_speed modifier remove grounded:slower_walk` and remove the jar.
- **TarkovCraft Core** (already included) also has **stamina** and **weight** systems, both on by default. Being overweight costs 15% speed and 12.5% jump height, and lowers step height and fall-damage tolerance. The settings are in `config/tarkovcraft_core`.

**Visuals (26.2):**
- **Iris 1.11.4** with **Complementary Reimagined r5.9.3**, on the pack's **Low** performance profile (no shadows, no SSAO, no coloured lighting).
- The shaderpack is downloaded from Modrinth by the `.mrpack`. Only the settings file is bundled: `overrides/shaders/shaderpacks/ComplementaryReimagined_r5.9.3.zip.txt`.
- The colour grade is cold and washed out, like Tarkov:
  - Saturation 0.70, pale colours 0.80.
  - Contrast 1.15, brightness 0.95.
  - Dark areas strongly desaturated (0.60).
  - Colour grading on: red 92%, green 100%, blue 108%.
  - Bloom 0.072, fewer stars.
- To tune it in-game, go to *Options → Video Settings → Shader Packs → Shader Pack Settings → Camera → Tonemap*. Press **K** to toggle shaders (Iris's default key; rebind it if it clashes with Medical System's health key).
- Not tested in the client yet. Check whether Medical System's pain and blood-loss screen effects still show with Iris on, how True Darkness looks, and FPS.

**People and wildlife (26.2):**
- **MCA Reborn 8.1.11** turns villagers into human survivors with names, families, moods, dialogue, trades and village guards.
  - In `overrides/tweaks/config/mca.json`, the Grim Reaper (`allowGrimReaper`) and inn cultists (`innSpawnsCultists`) are off, so there's nothing supernatural.
  - Zombie-bite infection stays on.
- **Hostile humans: Grounded Bandits** (our own mod, source in `bandits-addon/`, jar in `overrides/addons/mods/`). Bandits and bandit marksmen look like people, roam the surface in small groups, retreat when badly hurt and drop scavengeable supplies. Pillager outposts remain as bandit camps. The old roaming-pillager `spawner.json` rule is now empty (`[]`). Details are in `bandits-addon/README.md`.
- **The pack now bundles both custom jars** (Grounded Inventory 2.3.0 and Grounded Bandits 1.0.0) under `overrides/addons/mods/`.
- **Naturalist 2.0.7** adds real animals: bears, deer, boars, snakes, alligators and more, each in its own biome. The mammoth is turned off.
- **Limbs:** Grounded Tweaks 1.1.0 adds Medical System limb definitions for MCA humans (red blood) and MCA zombie villagers. Pillagers already had limbs. Naturalist animals have **no** limbs; they use normal health.
- **Tested on the server (2026-11-15):** 0 missing requirements, 20 TPS. `/tarkovcraft hurt` works on an MCA human and a zombie, and errors on a cow as expected. In Control read `spawner.json`.
- **Not tested in a world with a player yet:** bandit spawn rate, MCA villages, how Naturalist animals behave, and whether Attract to Sound affects pillagers.

**Starter kit (Grounded Tweaks 1.2.0):** every player gets a **Food Book** and a **Lunchbag** once, on first joining a world. The kit is granted through a hidden advancement (`grounded:starter_kit`), so it isn't given again after death. Since Grounded Tweaks 1.4.3 both can also be crafted: Spice of Life's own recipes use the `food` crafting category, which 26.2 doesn't have, so Tweaks ships fixed copies (`grounded:lunchbag`: 6 paper in a bag shape; `grounded:food_book`: book + potato). The Lunchbox and Golden Lunchbox upgrades work as before.

**Grounded Survival 1.0.0 (2026-11-16)** is our third custom mod. Its source is in `survival-addon/` and the jar is in `overrides/addons/mods/`; details are in `survival-addon/README.md`.
- **Realistic item weights** for TarkovCraft's weight system. Without them the Strength skill could never level, because vanilla items weighed 0 g.
- **Wooden bowl of water.**
- **Boiling water on a lit campfire:** water bottles, terracotta bowls and wooden bowls.
- **Low-thirst fruit and fruit juice.**
- **A fix for Saro's Fruit Trees,** whose worldgen didn't load on NeoForge 26.

The pack also gains two mods:
- **Better Days:** set to SEASON, so a full day lasts 40 minutes (twice vanilla), with summer days longer than winter ones. Config: `overrides/tweaks/config/betterdays-common.toml`.
- **Saro's Fruit Trees:** apple, orange and pear trees.

**Added 2026-11-16 (later the same day):**
- **Towns and Towers** (plus Cristel Lib and Cloth Config) adds more village types, pillager outposts (which work as bandit camps) and ships.
- **Camera Overhaul** (client only) makes the camera tilt and sway as you move.
- **Night sky** (Complementary settings): Unbound-style purple aurora on full-moon nights (`AURORA_STYLE_DEFINE=2`, `AURORA_CONDITION=1`), more stars (`NIGHT_STAR_AMOUNT=3`) and Unbound volumetric clouds (`CLOUD_STYLE_DEFINE=3`). For an aurora every night, set *Aurora C.* to *Every Night*. FPS cost not measured yet.
- **3D Skin Layers** and **Not Enough Animations** (client only).
- **Enhanced Visuals:** blood on the screen, a heartbeat at low health, blur from explosions. Check in game whether it overlaps with Medical System's screen effects.
- **Lithium:** game-logic optimisation.
- **Human spawn limits (In Control `spawn.json`, pack default since 2026-11-17):**
  - Survivors don't count as hostile, so the hostile cap and the 40% thinning missed them, and by day they filled the surface.
  - Bandits did count as hostile, so by day the 30-hostile cap (full of cave mobs) and the 40% thinning blocked most of them.
  - Each now has its own rules, checked before the general hostile rules. In Control uses the first rule that matches.
    - **Survivors:** at most 1 per player, 85% of natural spawns denied.
    - **Bandits and marksmen:** at most 2 per player, 75% denied, exempt from the hostile cap.
  - With the biome weights (bandits 4 + marksmen 2, survivors 3), a spawn attempt yields a bandit roughly three times as often as a survivor.
  - In Control reloads the rules without errors on the dedicated server. The real spawn rate needs checking in game.
- **Structure density: back to each mod's own rules.**
  - Sparse Structures `spreadFactor` is 1.0, with the mansion factor at 1. That is no extra spreading; the mod only keeps its per-structure salt so different structure sets overlap less.
  - The Towns and Towers tower frequency is back to its default of 0.2.
  - Tried 0.65 (2.4× denser) on 2026-11-16 and it felt too crowded in game.
- **Distant Horizons** (`overrides/tweaks/config/DistantHorizons.toml`):
  - `chunkGeneratorMode` changed from `FEATURES` to **`INTERNAL_SERVER`**. FEATURES generates everything except structures, so distant LODs never showed any buildings.
  - The trade-offs are more CPU use, and the chunks DH generates are saved to the world, so it uses more disk.
  - LOD radius was lowered from 256 to **128** chunks (2 km) to keep that in check. `limitIfServerUnhealthy` stays on.
  - The previous config is backed up as `DistantHorizons.toml.bak` in the profile.
- **More mobs:**
  - **Variants&Ventures** (plus Resourceful Lib) adds biome undead: Gelid, Murk, Thicket and Verdant.
    - All four are in Attract to Sound's `attractedEntities`.
    - They get Medical System limbs from Grounded Tweaks 1.3.0 (`vv_undead.json`, a copy of Medical System's humanoid template). Tested: `/tarkovcraft hurt` on the head lowered their health, and a cow returned "No health containers" as expected.
  - **Naturally Trimmed** adds random armour trims on mob gear.
  - **Aquaculture 2** adds biome fish and fishing gear.
  - **Ecologics** adds coconut crabs, squirrels, penguins and coconuts.
  - Server test: 20 TPS, started in 7 s.
- **Grounded Bandits 1.1.0** adds neutral **Survivors**. They look exactly like bandits and leave players alone unless attacked. They fight bandits and zombies, and trade medicine for a proper meal. See `bandits-addon/README.md`.
- **Terralith 2.6.4** (plus Lithostitched) adds about 100 biomes. Grounded Survival 1.0.2 now places fruit trees through biome tags (`#c:is_forest`, `#c:is_plains`, `#c:is_savanna`, and meadow for pears), so Terralith's forests and plains get them too. Server test: started in 9.5 s at 20 TPS with no errors. New biomes only appear in a new world or in unexplored chunks.
- **Field Medicine book (Grounded Tweaks 1.4.2):** an illustrated guide that every player gets once, through the hidden advancement `grounded:medical_guide_v3`.
  - 1.4.2 colours every icon white. Sprite glyphs are multiplied by the text colour, so in a book's black text they showed up as black silhouettes (see `AtlasGlyphProvider`).
  - Each fix uses a new advancement id, so players who already got an older book receive the new one as well. The old book shows raw translation keys and can be thrown away.
  - Pages:
    - A triage table with an icon for each item.
    - All 10 Medical System recipes drawn as 3×3 grids of item icons using 26.x text `object` components (atlas sprites), with the GUI slot sprite for empty cells.
    - An icon legend and what each item treats.
  - Ingredient names use the game's own translation keys, so the book follows the game language (Turkish or English).
  - Generated by `tweaks-src-tools/make_medical_book.py`.
  - Verified on the server: the item parses and the page components are stored correctly. Icons haven't been seen in the client yet.
- **e4mc 6.2.3** (client) lets the host *Open to LAN* and get a public `*.e4mc.link` address. Friends still need the same modpack. It isn't installed on the dedicated server.
- **Dedicated server (now at `C:\GroundedServer`, start it with `BASLAT.bat`), updated 2026-11-16:**
  - 35 server-side mods plus the 4 Grounded jars. Client-only mods were left out according to each project's Modrinth `server_side`: Sodium, Entity Culling, ImmediatelyFast, True Darkness, Iris, Camera Overhaul, 3D Skin Layers, Not Enough Animations, AmbientSounds and e4mc.
  - The pack configs were laid over `server/config`.
  - Backups: `world-backup-2026-11-16.zip`, `config-backup-2026-11-16.zip`, and the old jars in `mods-old-2026-11-16/`.
  - A boot test with the real world passed: "Done" in 1.7 s, 20 TPS.
  - The server was moved from the session folder to `C:\GroundedServer`. At the old path (about 252 characters, at Windows' 260 limit), Distant Horizons couldn't open its database (`SQLITE_CANTOPEN`). At the new path it starts its server levels for all three dimensions; boot test: 20 TPS.
- **Chunky was tested and removed.** With Distant Horizons 3.3.3 on 26.2, creating a new world hangs at "Selecting global world spawn"; Distant Horizons' `ChunkyAccessor` throws "Chunky is not loaded". Without Chunky the server started in 7 s.
- **Not available for 26.2:** the YUNG's mods (they stop at 26.1.2) and Handcrafted (it stops at 1.21.1).

**Death:** vanilla rules, with no grave mod. Dropped items despawn after 5 minutes. No health or damage multipliers. Set the world difficulty to Hard.

## Verification

**26.2 (2026-11-15).** I loaded the full stage-3 pack on a NeoForge 26.2.0.93 server with Java 25 (client-only mods removed). Settings: new world, Hard, night and thunder, force-loaded chunks.
- The loader found **0 missing requirements**, after I added GlitchCore (see below).
- It started in about 4 s, ran at **20 TPS** (about 0.6 ms per tick), and shut down cleanly with no crash reports.
- `keep_inventory` is confirmed `false`. On 26.x the gamerule is `keep_inventory`, not `keepInventory`.
- Thirst Was Taken 2 logged *"Serene Seasons found, thirst follows the season"*. `/season set early_winter` works.
- In Control read `spawn.json` and turned on its Serene Seasons support.
- `build_pack.py` adds GlitchCore explicitly, because Serene Seasons' Modrinth listing doesn't declare it even though the mod requires it. Without it the server refuses to start.

**26.2 known issues** (the cost of being on the newest version):
- **Cold Sweat is an alpha build.** Three of its advancements fail to load, including the one that unlocks the waterskin recipe in the recipe book. The recipe still exists, so look it up in JEI.
- **Spice of Life: Onion:** its own Food Book and Lunch Bag recipes still log a parse error (`Unknown element name:food`). This is harmless: Grounded Tweaks 1.4.3 adds working copies.
- **In Control:** its global loot-modifier file fails to load. That only affects `loot.json` rules, which this pack doesn't use. The spawn rules loaded fine.
- **Medical System 2.14.0** is one minor version behind the 1.21.1 build (2.14.2).
- **Harmless log noise:** Linux and macOS networking libraries, Windows performance counters, and mixin warnings for optional mods that aren't installed.

**1.21.1 fallback:** stages 3a and 3b each passed the same world-load test at 20 TPS. Cold Sweat's thirst integration is off there, because it needs Thirst Was Taken 3.x, which doesn't exist.

**Not tested on either version:** natural mob spawning and AI (they need a player online), and the client side (Sodium, HUD overlap, frame rate on the RTX 3070 Laptop GPU).

## In-game test plan

1. **Medical:**
   - Get a fracture from a fall and apply a splint.
   - Fix a light bleed with a bandage and a heavy bleed with a tourniquet.
   - Press `O` and open the Health tab to see each limb.
   - Let go of a bandage halfway and confirm the healing was only partial.
2. **Survival:**
   - Check whether the Cold Sweat, Thirst Was Taken 2, Medical System and AppleSkin HUDs overlap.
   - Purify water.
   - Spend a winter night (`/season set early_winter`) high up a mountain and watch your temperature.
3. **AI:**
   - Make noise near a cave and see whether mobs investigate.
   - Fight groups of zombies and skeletons.
   - Keep the nearby mob count under about 60 at night.
4. **Performance:** check FPS at 12 chunks render distance and 8 simulation distance.
