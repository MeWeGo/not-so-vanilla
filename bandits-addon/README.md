# Grounded Bandits

A NeoForge **26.2** mod for the Grounded pack: hostile human bandits that roam the overworld in small groups, plus neutral survivors who look exactly like them.

## What it adds

**Two entity types, one class (`dev.grounded.bandits.entity.Bandit`).** Sharing a class means vanilla's "alert others" makes melee bandits and marksmen call each other for help.

- **Bandit** (`grounded_bandits:bandit`): melee.
  - Random weapon: stone or iron sword, stone or iron axe.
  - Patchy leather armour; a 20% chance the chest piece is chainmail.
- **Bandit Marksman** (`grounded_bandits:bandit_marksman`): bow. Keeps its distance and shoots, using the skeleton's bow logic.

**Grounded rules:**
- Normal human stats: 20 HP, speed 0.27, 2 base damage plus the weapon. No inflated health.
- **Retreats below 35% HP**, running from the player instead of fighting to the death.
- Silent while idle, so no ambient chatter gives them away. They use the player hurt and death sounds.
- Hurting one bandit alerts the whole group.
- Targets players and villagers, including MCA humans. MCA guards target them back (set in the pack's `mca.json`).
- Looks like a person: the vanilla player model with one of the 9 default player skins that ship with the game. No third-party textures are used.

**Spawning** (`data/grounded_bandits/neoforge/biome_modifier/add_bandits.json`):
- All overworld biomes, in the MONSTER category, so bandits count towards the hostile cap and In Control's `spawn.json` thinning applies to them.
- Bandit weight 4 in groups of 2–3; Marksman weight 2 in groups of 1–2.
- Surface only (natural spawns need open sky), and none on Peaceful.

**Loot** (`data/grounded_bandits/loot_table/entities/`):
- Bread, iron nuggets and leather.
- Medical System supplies: bandage 35%, painkillers 12%, tourniquet 6%.
- Marksmen also drop arrows.
- Equipment drop chance: 15% for the weapon, 10% for each armour piece.

**Medical System limbs** (`data/grounded_bandits/tarkovcraft/health/bandits.json`): the same head, body and legs layout as vanilla humanoids, with red blood.

## Survivors (1.1.0)

**Survivor** (`grounded_bandits:survivor`, class `entity.Survivor`) is a neutral human. It shares the bandits' skins, loadout code (`HumanCombat`) and renderer, so you can't tell friend from foe until one of them acts.
- **Leaves players alone** unless attacked. Hurting one turns its companions on you as well.
- **Fights bandits and zombies on sight.** Bandits also target survivors, so fights can break out without you.
- **Retreats** below 35% HP from whoever it's fighting.
- **Gear:** 30% carry a bow, otherwise a melee weapon, plus the same patchy armour as bandits.
- **Trade:**
  - Right-click it holding a proper meal (4+ hunger) and it takes one and drops medicine: bandage 50%, painkillers 25%, splint 15%, tourniquet 10%.
  - Then it has nothing left for half a vanilla day (12000 ticks, saved with the entity), and refuses with a message.
- **Spawning:** the same surface-only rule as bandits, weight 3, in groups of 1–2. Natural spawns are off on Peaceful.
- **Limbs:** Medical System limbs (added to `bandits.json`).
- **Loot:** bread, steak, leather, a bandage (25%) and arrows (30%).
- **Not added** to Attract to Sound or MCA guard targets.
- **Verified on the server:**
  - A survivor and a bandit placed 5 blocks apart fought each other; both lost health.
  - Equipment, limbs and loot work.
- **Not verified:** whether a survivor fights a zombie (the test zombie was in an unloaded chunk), trading with a real player, and natural spawn rates.

## Pack integration (lives in the pack, not in this jar)

- **Attract to Sound:** both types are added to `attractedEntities` in `config/soundattract/general.toml`.
- **MCA:** `guardsTargetEntities` includes both types at priority 4.
- **In Control:** the old roaming-pillager `spawner.json` rule was removed, since bandits replace it. Pillager outposts remain as bandit camps.

## Build

```
./gradlew build      # -> build/libs/grounded_bandits-neoforge-1.1.0+26.2.jar
```

## Verified (2026-11-15, NeoForge 26.2.0.93 dedicated server with the full pack)

- 0 missing requirements, 20 TPS, no crash reports.
- `/summon` works for both types. Random skins and loadouts were confirmed with `/data get`: iron or stone swords and axes, leather pieces, chainmail, and a bow on the marksman.
- `/tarkovcraft hurt <bandit> head|legs …` succeeds, so the limb definitions load.
- A killed bandit dropped `medsystem:bandage`, so the loot table loads.
- Attract to Sound's config keeps the bandit entries after loading, and the biome modifier loaded without errors.

## Not verified yet

- **Rendering in the real client:** skin, armour layer and bow pose.
- **Natural spawn frequency:** that needs a player in a world.
- **Combat feel:** retreat behaviour, marksman accuracy, and whether Attract to Sound actually drives them.
