# Grounded Survival

A NeoForge **26.2** mod for the Grounded pack. It requires TarkovCraft Core and Thirst Was Taken 2 (TWT2).

## What it adds

**Item weights** (`ItemWeights.java`):
- Every item without a `tarkovcraft_core:weight` gets a realistic weight in grams. Medical System and TarkovCraft items keep their own.
- Without this, vanilla items weigh 0 g. The 20 kg limit was then never reached, and the **Strength** skill (which trains while overweight and sprinting) could not level.
- Examples:
  - Iron chestplate 4 kg; a full iron set is about 9.7 kg; leather is about 28% of iron.
  - Iron pickaxe 1.3 kg.
  - Cobblestone 150 g, so a stack of 64 is about 9.6 kg. Ore 300 g, log 120 g, plank 40 g.
  - Water bucket 8 kg, water bottle 450 g.
  - Arrow 25 g, ingot 120 g.
- Unknown modded items fall back to a guess based on stack size: 500 g if they stack to 1, 150 g up to 16, otherwise 30 g.

**Bowl of Water** (`grounded_survival:water_bowl`):
- Right-click water with a plain wooden bowl to fill it.
- The water's grade comes from TWT2's sampling (0 dirty, 1 murky, 2 clean, 3 pure) and is stored in TWT2's own `thirstwastaken2:water_purity` component.
- Drinking gives TWT2's plain-water thirst. Quenched scales with purity, sickness rolls through TWT2, and you get the bowl back.
- Sea water can't be carried in a wooden bowl.

**Boiling on a campfire** (data only, `data/grounded_survival/recipe/boil_*`):
- Water bottles, TWT2 terracotta water bowls and wooden water bowls placed on a lit campfire boil in 30 s, from dirty or murky to clean.
- Wooden bowls also boil in a furnace or smoker.
- Every boiling recipe follows TWT2's `enableFurnaceBoiling` setting.

**Fruit**:
- **Fruit Juice:** glass bottle + 3 fruits (tag `grounded_survival:juice_fruits`). It gives 3 thirst and 0 quenched.
- Saro's orange and pear give 2 thirst and 0 quenched (set in `data/grounded_survival/thirstwastaken2/drinks/fruit.json`). Fruit only takes the edge off thirst; you still need to boil water.
- **Fixed fruit-tree worldgen:** Saro's Fruit Trees 1.0.4 ships its biome modifiers in the old `forge/` folder, which NeoForge 26 ignores. `data/grounded_survival/neoforge/biome_modifier/fruit_trees_*.json` re-adds them, and they only load when that mod is present. Since 1.0.2 they use biome tags (`neoforge:or` of `#c:is_forest`, `#c:is_plains`, `#c:is_savanna`, and `minecraft:meadow` for pears), so modded biomes such as Terralith's are included. The trees themselves roll a 0.3% chance per chunk, so groves are rare.

**Fruit food fix (1.0.1):**
- Saro's orange and pear shipped with a saturation modifier of 24 (vanilla apple: 0.3), which gave 192 saturation per fruit.
- Spice of Life: Onion scores a food's diversity as (nutrition / 5) × (saturation / 6), so one pear was worth about 25.6 diversity.
- `FoodFixes.java` resets both fruits to apple values (4 nutrition, 2.4 saturation), which gives a diversity of 0.32 (steak is about 3.4).
- Verified on the server: the pear's `minecraft:food` matches the apple's values and no longer 192.

## Build

```
./gradlew build      # -> build/libs/grounded_survival-neoforge-1.0.0+26.2.jar
```

## Verified (2026-11-16, NeoForge 26.2.0.93 dedicated server with the full pack)

- Weights, checked with `execute if items … *[tarkovcraft_core:weight=N]`:
  - Iron chestplate 4000, cobblestone 150, leather helmet 420, water bowl 450.
  - The Medical System bandage kept its own weight.
- On a lit campfire, a water bottle (purity 0), a terracotta bowl (0) and a wooden bowl (0 and 1) all came out at purity 2.
- With Better Days set to SEASON at 40 minutes, time ran at about half speed.
- The Saro's tree template places, and the biome modifiers load without errors. 20 TPS.

## Not verified yet (needs a player)

- Filling and drinking a bowl in game.
- Fruit juice crafting and drinking.
- Strength levelling while overweight.
- The textures in the client, and natural fruit groves in a new world.
