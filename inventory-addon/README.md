# Grounded Inventory (NeoForge 26.2)

Adds an **Inventory** tab to TarkovCraft Core's character screen, the same screen that holds
Statistics, Health (Medical System) and Skills.

- Jar: `build/libs/grounded_inventory-neoforge-2.3.0+26.2.jar`
- Requires: Minecraft 26.2, NeoForge ≥ 26.2.0.57 (built on 26.2.0.93), TarkovCraft Core 2.11.x.
  Medical System is optional.
- Install on: the client. For drag-and-drop healing in multiplayer, the **server needs it too**. The packet is optional, so a client can still join a server without the mod; drag-healing is then shown as unavailable.

## 2.3 changes: Tarkov-style drag-and-drop healing (Medical System)
- **How to use it:** pick up a Medical System item (bandage, dressing, tourniquet, splint, kits, painkillers, blood bag and so on) and drop it on a limb.
  You can drag it with the mouse held down, or click to pick it up and then click a limb. While a medical item is on the cursor,
  a limb silhouette (Medical System's own body layout, coloured by limb health) replaces the character model.
- **Hover feedback:** valid limbs get a green outline. The hovered limb gets white if valid, red if not, and a tooltip says why:
  "Nothing this item can treat on this limb", "Limb destroyed: needs a surgery kit", "Limb is at full health", "Already using an
  item", "Stand up to treat yourself" and so on. It also lists what the item does, using Medical System's own recovery labels,
  which include its limb restrictions (for example arms/legs only).
- **It runs Medical System's real treatment.** A client→server packet (`grounded_inventory:drag_heal`) carries only the limb code. The server:
  1. re-checks everything with Medical System's own `HealItemAttributes.canUseOn` / `canUseOnLimb` (the same checks as its limb-select packet);
  2. moves the cursor item into the main hand (the old main-hand item goes to a free slot, or the drop is refused);
  3. writes the chosen limb as the item's `INTERACTION_TARGET` component (as Medical System's limb-select screen does);
  4. calls vanilla `ServerPlayerGameMode.useItem`.

  From there Medical System's `InteractableItem`/`HealingItem` code does everything: use time, cycle healing, item damage
  and consumption, effect removal on finish, cooldown. Items that work on the whole body (painkillers, morphine and other
  "simple" items, blood bags) are simply used.
- **Progress:** the treated limb gets a yellow outline, with a progress bar and time left under the silhouette. Afterwards it shows
  "Treatment complete" or "Treatment stopped".
- **The screen stays open while treating.** Closing the inventory cancels the treatment, through Medical System's normal
  release-use path, because vanilla only checks the use key while no screen is open.
- **Interruptions** (only for treatments started by drag-and-drop; normal right-click use is unchanged): combat damage (from an
  entity or projectile), falls, explosions, attacking, or sprinting stop the treatment. Switching the item away stops it too (vanilla).
  Bleeding, hunger, thirst, cold and poison damage do **not** interrupt, otherwise you could never bandage a bleed.
  Medical System keeps the healing cycles already completed, so an interrupted bandage heals partially. Bleed/fracture removal
  happens only on finish, so an interrupted tourniquet does nothing. A 0.5 s cooldown follows, the same as Medical System's own cancel.
- Normal right-click hold-to-use is untouched.
- Not done: healing teammates by dragging onto them.

## 2.2 changes
- **Diet row** in STATUS (Spice of Life: Onion), under Hunger. The vanilla inventory's Food Book button doesn't appear on this screen, so this replaces it.
  - Bar and number: food diversity from `SOLOnionAPI.getFoodCapability(player).foodDiversity(player)`, with a tick at the next
    benefit threshold. "+N" means N benefits are active. The row turns red if a detriment is active.
  - Tooltip: active benefits and detriments (named the way the Food Book names them), the next threshold, and recently eaten foods
    from Onion's tracked queue.
  - Click the row to open Onion's own Food Book.
  - Thresholds come from Onion's config (`SOLOnion.CONFIG`), judged with the same rule the Food Book uses. That's an internal
    class, so if a later Onion update changes it, the row hides itself instead of crashing the screen.
- Active mob effects from Onion benefits also show in the EFFECTS column.

## 2.1 changes
- **EFFECTS column** to the right of the panel, where vanilla puts its effect list. It's part of the screen's width, so JEI and click-outside handling account for it.
  - Medical System status effects (bleeds, fractures, pain, painkillers and so on) come first, read from each limb's synced status-effect map.
    Each row shows icon, name, limb and remaining time. The tooltip matches the Health tab's (extra info, duration, "Painful", tags).
    Effects on the root limb (the head) are labelled "Head or whole body", because Medical System stores whole-body effects there too.
  - Vanilla mob effects follow, with icon, name, level and remaining time. NeoForge's effect hooks apply: hidden effects,
    custom icons and tooltip events.
  - If there are too many rows to fit, the last row shows "+N" and its tooltip lists the rest. On very narrow screens the column shrinks to icons only.

## v2 changes
- **E opens this screen.** Anything that opens the plain survival `InventoryScreen` (exact class) gets the Inventory tab instead.
  Creative mode keeps the vanilla creative inventory. The 2×2 crafting grid is in the Equipment block. Turn this off with `replaceVanillaInventory`.
- **STATUS block** under the equipment, Tarkov-style thin bars with vanilla icons and numbers. Hover a row for details.
  - Health: Medical System's synced health container, one bar segment per limb. The tooltip lists each limb's HP, or "Destroyed".
    It uses the same number scale as the Health tab. Without Medical System it falls back to vanilla health.
  - Hunger: vanilla food level, with saturation as a thin line along the bar.
  - Hydration: Thirst Was Taken 2's `ThirstApi.thirst/quenched/maxThirst` (quenched as the thin line). Hidden if thirst is disabled for the player.
  - Body temperature: Cold Sweat's `Temperature.get(player, BODY)`. The bar is centred on 0, with danger marks at ±100.
  - Stamina: TarkovCraft Core's movement stamina (`EnergySystem.MOVEMENT_STAMINA`).
  - Armour and toughness are shown under the weight bar.
  - Each integration is optional and only loaded when its mod is installed.
- **HUD bars hidden** (`hideHudBars = true` by default) by cancelling these GUI layers: `minecraft:player_health`, `food_level`,
  `armor_level`, `thirstwastaken2:thirst_bar`, `cold_sweat:body_temp`, `cold_sweat:vague_temp`. You can edit the list.
  Air, XP, hotbar, TarkovCraft stamina and Medical System's top-left limb overlay stay on screen.
  Cold Sweat has its own toggles ("Show Body Temperature Icon/Readout" in `config/coldsweat/client.toml`), but they're
  cancelled here too so one switch controls everything. Thirst Was Taken 2 and vanilla have no HUD toggles.
- Config: `config/grounded_inventory-client.toml`, or in-game under Mods → Grounded Inventory → Config.
- **Design trade-off:** with the HUD bars hidden, you can only check your condition by opening the inventory. That fits the
  "you don't know how bad it is" feel, but makes combat harder. Being low on HP mid-fight is only visible through
  damage feedback. Set `hideHudBars = false` if that's too harsh.
- Known gaps: there's no vanilla recipe-book button. JEI's "+" recipe-transfer for the 2×2 grid is registered for the
  vanilla screen, so it probably won't show here, although JEI's item list should. Buttons other mods inject into the
  vanilla `InventoryScreen` won't appear.

## What it does (v1)

- Adds a tab (order 20: Statistics → **Inventory** → Health → Skills), registered with
  `CoreNavigators.CHARACTER_NAVIGATION_PROVIDER` the same way Medical System registers Health.
  The tab only appears on your own profile.
- Layout styled after the Health tab: same blurred background, profile title and tab bar.
  - **Equipment** block: your player model in the middle (it follows the mouse), helmet, chest,
    legs and boots slots on the left, and the 2×2 crafting grid, result slot and offhand slot on the right.
  - **Pockets**: the 27-slot main inventory as a 9×3 grid, with the 9-slot **hotbar** below it.
  - **Weight**: total carried weight against the limit (`tarkovcraft_core:weight_limit`
    attribute), with a bar coloured green when fine, yellow when overweight and red at ≥2× the limit.
    An OVERWEIGHT tag appears when you're over. This uses `WeightSystem` and the synced weight attachment.
  - Item tooltips show weight through TarkovCraft Core's own tooltip hook, so every item says its stack weight.
- **Item handling is real.** The screen shows `player.inventoryMenu` (container 0, which the
  server always has open) through a client-side proxy menu that only moves the slots on screen.
  Clicks go through vanilla `handleContainerInput` and the normal vanilla click packet. Armour
  rules, offhand, shift-click, drag-splitting, number keys / F swap, double-click collect and
  dropping by clicking outside all behave as in the vanilla inventory, and it works in multiplayer
  without custom packets.
- Optional key **Open Inventory tab** (TarkovCraft category, unbound by default). Bind it to J if you like.

## How it was tested

| Check | Result |
|---|---|
| Compiles against NeoForge 26.2.0.93 + TarkovCraft Core 2.11.0+26.2 (Java 25) | ✅ |
| Loads on a dedicated-server dist (NeoForge 26.2.0.93 game test server, `runPackCheck`) next to all 18 of the pack's server-side jars (everything except Sodium, ImmediatelyFast and Entity Culling), with 0 missing requirements and no errors from this mod; the server reached "All 1 required tests passed" and shut down cleanly | ✅ |
| Real `server.jar` dedicated server | not run, because that needs accepting the Minecraft EULA (`eula.txt`). The game test server loads the same server-dist mod set without it |
| v1: the user opened the tab in the real client and liked the look | ✅ (user feedback) |
| v2: dev client with all 21 pack jars starts to the title screen, no errors from this mod, client config generated | ✅ |
| v2: E replacement, status bars, hidden HUD, in a world | ❌ **not tested yet**: needs an in-game check |
| 2.3: server loads the drag-heal packet and event listeners next to the pack (gameTestServer), and the dev client reaches the title screen with all pack jars | ✅ |
| 2.3: actual drag-and-drop treatment, validity tooltips, progress, interruptions, in a world | ❌ **not tested**: needs an in-game check |

### Manual client test checklist (2.3, drag healing)
- Get hurt in survival, open the inventory and drag a bandage onto the hurt limb. Check the green/white outline, the progress bar,
  that the limb heals cycle by cycle, and that the bandage is used up.
- Hover a tourniquet over the head (red, with a reason). Then use it on a bleeding arm or leg.
- Drag a splint onto a limb without a fracture (red). Drag a surgery kit onto a healthy limb (red) and onto a destroyed limb (works).
- Start a bandage, then let a mob hit you: "Treatment interrupted: took damage", with only partial healing. Start one and close the inventory: it stops.
- Painkillers or morphine dropped on any limb should just be used.
- On a dedicated server with the mod on both sides, repeat one bandage, and check for item duplication after relogging.

### Manual client test checklist (2.2)
- Eat a few different foods: the Diet number rises, and the tooltip lists recent foods and any active benefits.
- Click the Diet row: Onion's Food Book opens.

### Manual client test checklist (2.1)
- Drink a potion, or stand by a beacon: the effect appears under EFFECTS with level and time, and hovering shows details.
- Get a bleed or fracture (fall damage, combat): it appears with the limb name. A painkiller shows its timer.
- Check the column fits at your GUI scale, and whether JEI's item list still has room.

### Manual client test checklist (v2)
- Press E in survival: the Tarkov inventory opens. Press E again: it closes. In creative, E opens the vanilla creative inventory.
- Check that the bottom HUD no longer shows hearts, food, armour, the thirst bar or the Cold Sweat body-temperature icon.
- Get hurt, eat, drink and get cold or hot, then check the STATUS bars and their tooltips.
- With JEI: is the item list beside the screen, and do recipe lookups (R/U) work?

### Manual client test checklist (v1)
1. Open the character screen (TarkovCraft key, P by default; rebind it to J because P clashes
   with Social Interactions) and click **Inventory**, or bind "Open Inventory tab".
2. Move items between pockets and the hotbar. Equip armour by clicking and by shift-clicking.
   Put a shield in the offhand.
3. Check that the weight numbers change as you pick items up, and that tooltips show weight.
4. Switch to the Health tab while holding an item on the cursor: the item should go back into your inventory.
5. On a server: repeat step 2 and relog to confirm nothing duplicates or vanishes.

## Ideas for v2 (out of scope)
- Tarkov multi-cell item sizes (a grid where items take up W×H cells), with item rotation.
- Rigs and backpacks as equippable containers that add their own grids, which needs real
  server-side menus and item storage components.
- Scaling the layout with GUI height the way the Health tab does, plus per-section weight subtotals.
- A small weight read-out on each slot, and a search/sort button.

## Build
```
gradlew build            # JAVA_HOME = a JDK 25
gradlew runPackCheck     # game test server; loads jars put in run-gametest/mods
gradlew runPackClient    # dev client; loads jars put in run-client/mods
```
