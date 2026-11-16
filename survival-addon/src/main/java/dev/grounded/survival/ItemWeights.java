package dev.grounded.survival;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import tnt.tarkovcraft.core.common.init.CoreItemDataComponents;

/**
 * Gives every item without a weight a realistic one (grams) for TarkovCraft Core's weight system.
 * Items that already have a weight (Medical System's supplies) keep theirs.
 *
 * <p>Balance target: the default limit is 20 kg. A full iron kit (armour + four tools) is about 14 kg, so a stack of
 * stone or ore on top of it makes you overweight. Overweight + sprinting is what trains the Strength skill.
 */
final class ItemWeights {

    private ItemWeights() {
    }

    /** Weights decided in the predicate pass, applied in the initializer pass. */
    private static final Map<Item, Integer> DECIDED = new ConcurrentHashMap<>();

    static void assign(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(ItemWeights::decide,
                (builder, registries, item) -> builder.set(CoreItemDataComponents.WEIGHT.get(), DECIDED.getOrDefault(item, 0)));
    }

    private static boolean decide(Item item, DataComponentGetter components) {
        if (components.get(CoreItemDataComponents.WEIGHT.get()) != null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getNamespace().equals("medsystem") || id.getNamespace().equals("tarkovcraft_core")) {
            return false; // their own weights are authoritative
        }
        int grams = Math.max(1, weigh(id.getNamespace(), id.getPath(), item, components));
        DECIDED.put(item, grams);
        return true;
    }

    static int weigh(String ns, String p, Item item, DataComponentGetter c) {
        // ---- exact items first ----
        Integer exact = exact(p);
        if (exact != null && (ns.equals("minecraft") || ns.equals("thirstwastaken2") || ns.equals("grounded_survival"))) {
            return exact;
        }
        if (ns.equals("thirstwastaken2")) {
            return thirst(p);
        }

        // ---- worn armour ----
        if (p.endsWith("_helmet")) return (int) (1500 * metal(p));
        if (p.endsWith("_chestplate") || p.endsWith("_tunic")) return (int) (4000 * metal(p));
        if (p.endsWith("_leggings") || p.endsWith("_pants")) return (int) (3000 * metal(p));
        if (p.endsWith("_boots")) return (int) (1200 * metal(p));
        if (p.endsWith("_horse_armor")) return (int) (12000 * metal(p));

        // ---- tools and weapons ----
        if (p.endsWith("_sword")) return (int) (1100 * metal(p));
        if (p.endsWith("_axe")) return (int) (1400 * metal(p));
        if (p.endsWith("_pickaxe")) return (int) (1300 * metal(p));
        if (p.endsWith("_shovel")) return (int) (900 * metal(p));
        if (p.endsWith("_hoe")) return (int) (700 * metal(p));
        if (p.endsWith("_spear")) return (int) (1200 * metal(p));
        if (p.endsWith("_knife")) return (int) (300 * metal(p));
        if (c.has(DataComponents.TOOL) || c.has(DataComponents.WEAPON)) return 1000;

        // ---- metals, minerals, materials ----
        if (p.endsWith("_nugget")) return 12;
        if (p.startsWith("raw_") && p.endsWith("_block")) return 1350;
        if (p.startsWith("raw_")) return 150;
        if (p.endsWith("_ingot")) return p.contains("gold") ? 200 : p.contains("netherite") ? 250 : 120;
        if (p.endsWith("_dye")) return 10;
        if (p.endsWith("_seeds") || p.endsWith("_seed")) return 5;
        if (p.endsWith("_spawn_egg")) return 60;
        if (p.endsWith("_bucket")) return 8000;
        if (p.endsWith("_boat") || p.endsWith("_raft")) return 15000;
        if (p.endsWith("_minecart")) return 20000;
        if (p.endsWith("_banner_pattern") || p.endsWith("_pottery_sherd") || p.endsWith("_smithing_template")) return 80;
        if (p.startsWith("music_disc_")) return 150;

        // ---- food ----
        if (c.has(DataComponents.FOOD)) {
            if (p.contains("stew") || p.contains("soup")) return 500;
            if (p.contains("beef") || p.contains("porkchop") || p.contains("mutton") || p.contains("venison")) return 300;
            if (p.contains("chicken") || p.contains("rabbit")) return 350;
            if (p.contains("cod") || p.contains("salmon") || p.contains("fish")) return 250;
            if (p.contains("berries") || p.contains("berry") || p.contains("grape")) return 20;
            if (p.contains("pie") || p.contains("bread") || p.contains("loaf")) return 300;
            if (p.contains("cookie")) return 30;
            return 150;
        }
        if (c.has(DataComponents.CONSUMABLE)) return 400; // drinks, potions

        // ---- placeable blocks ----
        if (item instanceof BlockItem) {
            return block(p);
        }

        // ---- everything else: guess from stack size ----
        Integer max = c.get(DataComponents.MAX_STACK_SIZE);
        int stack = max == null ? 64 : max;
        return stack == 1 ? 500 : stack <= 16 ? 150 : 30;
    }

    /** Material factor relative to iron. */
    private static double metal(String p) {
        if (p.startsWith("leather_")) return 0.28;
        if (p.startsWith("chainmail_")) return 0.7;
        if (p.startsWith("wooden_")) return 0.4;
        if (p.startsWith("stone_")) return 0.85;
        if (p.startsWith("copper_")) return 0.95;
        if (p.startsWith("golden_")) return 1.2;
        if (p.startsWith("diamond_")) return 1.05;
        if (p.startsWith("netherite_")) return 1.3;
        if (p.startsWith("turtle_")) return 0.5;
        return 1.0;
    }

    private static Integer exact(String p) {
        return switch (p) {
            // containers and water
            case "bucket" -> 1000;
            case "glass_bottle" -> 150;
            case "potion", "splash_potion", "lingering_potion", "experience_bottle", "honey_bottle", "ominous_bottle" -> 450;
            case "bowl" -> 100;
            case "water_bowl" -> 450;
            case "fruit_juice" -> 400;
            // gear
            case "bow" -> 700;
            case "crossbow" -> 2500;
            case "shield" -> 4500;
            case "trident" -> 2500;
            case "mace" -> 3500;
            case "elytra" -> 1500;
            case "fishing_rod", "carrot_on_a_stick", "warped_fungus_on_a_stick" -> 400;
            case "shears" -> 300;
            case "flint_and_steel" -> 100;
            case "brush" -> 150;
            case "spyglass" -> 400;
            case "compass", "recovery_compass" -> 150;
            case "clock" -> 200;
            case "lead" -> 100;
            case "saddle" -> 3000;
            case "totem_of_undying" -> 300;
            case "arrow", "spectral_arrow", "tipped_arrow" -> 25;
            case "firework_rocket" -> 50;
            case "snowball" -> 50;
            case "egg", "blue_egg", "brown_egg" -> 60;
            case "ender_pearl" -> 80;
            case "wind_charge", "fire_charge" -> 60;
            // materials
            case "diamond", "emerald" -> 20;
            case "amethyst_shard", "quartz", "prismarine_shard", "prismarine_crystals", "echo_shard" -> 20;
            case "coal", "charcoal" -> 40;
            case "redstone", "glowstone_dust", "gunpowder", "sugar", "lapis_lazuli", "blaze_powder", "bone_meal" -> 10;
            case "flint" -> 30;
            case "stick", "blaze_rod", "breeze_rod" -> 20;
            case "string", "feather" -> 5;
            case "leather" -> 150;
            case "rabbit_hide" -> 50;
            case "paper" -> 5;
            case "book", "writable_book", "written_book", "enchanted_book" -> 300;
            case "map", "filled_map" -> 10;
            case "bone" -> 80;
            case "slime_ball", "magma_cream" -> 50;
            case "clay_ball" -> 100;
            case "brick", "nether_brick" -> 150;
            case "wheat" -> 20;
            case "netherite_scrap" -> 200;
            case "armadillo_scute", "turtle_scute" -> 50;
            case "ink_sac", "glow_ink_sac" -> 30;
            case "nautilus_shell", "heart_of_the_sea" -> 200;
            case "nether_star" -> 300;
            default -> null;
        };
    }

    /** Thirst Was Taken 2 vessels and machines. */
    private static int thirst(String p) {
        return switch (p) {
            case "waterskin" -> 350;
            case "copper_canteen" -> 500;
            case "iron_flask" -> 600;
            case "diamond_flask" -> 700;
            case "netherite_flask" -> 900;
            case "clay_bowl", "terracotta_bowl" -> 300;
            case "terracotta_water_bowl" -> 600;
            case "salt_water_bottle" -> 450;
            case "salt_water_bucket" -> 8000;
            case "copper_hanging_pot", "iron_hanging_pot" -> 2000;
            case "copper_distiller", "distiller_boiler", "cooling_tub", "brick_firebox" -> 6000;
            case "copper_pipe" -> 300;
            default -> 200;
        };
    }

    /** Grams per placeable block. Building is still possible, but you can't haul a quarry in your pockets. */
    private static int block(String p) {
        double shape = 1.0;
        if (p.endsWith("_slab")) shape = 0.5;
        else if (p.endsWith("_stairs") || p.endsWith("_wall")) shape = 0.75;
        else if (p.endsWith("_pane") || p.endsWith("_carpet") || p.endsWith("_pressure_plate") || p.endsWith("_button")) shape = 0.25;

        // storage blocks: nine of the item
        switch (p) {
            case "iron_block", "copper_block" -> { return 1080; }
            case "gold_block" -> { return 1800; }
            case "netherite_block" -> { return 2250; }
            case "diamond_block", "emerald_block", "lapis_block", "redstone_block" -> { return 180; }
            case "coal_block" -> { return 360; }
            case "anvil", "chipped_anvil", "damaged_anvil" -> { return 15000; }
            case "furnace", "smoker" -> { return 1500; }
            case "blast_furnace" -> { return 3000; }
            case "cauldron", "hopper" -> { return 2500; }
            case "obsidian", "crying_obsidian" -> { return 400; }
            case "tnt" -> { return 600; }
            case "bookshelf", "chiseled_bookshelf" -> { return 1500; }
            case "chest", "trapped_chest", "barrel" -> { return 500; }
            case "ender_chest" -> { return 2000; }
            case "crafting_table", "cartography_table", "fletching_table", "smithing_table", "loom" -> { return 300; }
            case "torch", "soul_torch", "redstone_torch", "copper_torch" -> { return 20; }
            case "lantern", "soul_lantern" -> { return 300; }
            case "cake" -> { return 900; }
            case "scaffolding" -> { return 50; }
            default -> { }
        }
        if (p.endsWith("_bed")) return 1500;
        if (p.endsWith("_shulker_box") || p.equals("shulker_box")) return 1000;
        if (p.endsWith("_ore")) return p.startsWith("deepslate_") ? 330 : 300;
        if (p.endsWith("_door") || p.endsWith("_trapdoor")) return (int) (400 * (p.contains("iron") ? 3 : 1));
        if (p.contains("rail")) return 100;
        if (p.endsWith("_sign") || p.endsWith("_banner") || p.endsWith("candle")) return 100;
        if (p.endsWith("_fence") || p.endsWith("_fence_gate")) return 80;
        if (p.contains("glass")) return (int) (80 * shape);
        if (p.contains("wool")) return (int) (40 * shape);
        if (p.contains("leaves") || p.contains("sapling") || p.contains("flower") || p.contains("tulip") || p.contains("grass")
                || p.contains("fern") || p.contains("vine") || p.contains("mushroom") || p.contains("roots") || p.contains("propagule")) {
            return 15;
        }
        if (p.endsWith("_log") || p.endsWith("_wood") || p.endsWith("_stem") || p.endsWith("_hyphae")) return 120;
        if (p.contains("planks") || p.contains("bamboo") || p.contains("_wood")) return (int) (40 * shape);
        if (p.contains("snow") || p.contains("ice")) return 100;
        if (p.contains("dirt") || p.contains("mud") || p.contains("clay") || p.contains("soul_s") || p.contains("podzol")
                || p.contains("mycelium") || p.contains("farmland") || p.equals("moss_block")) {
            return (int) (120 * shape);
        }
        if (p.contains("sand") && !p.contains("sandstone")) return 130;
        if (p.contains("gravel") || p.contains("concrete_powder")) return 140;
        if (p.contains("netherrack") || p.contains("end_stone") || p.contains("pumpkin") || p.contains("melon") || p.contains("hay")) {
            return (int) (100 * shape);
        }
        if (p.contains("stone") || p.contains("cobble") || p.contains("deepslate") || p.contains("granite") || p.contains("diorite")
                || p.contains("andesite") || p.contains("tuff") || p.contains("basalt") || p.contains("brick") || p.contains("concrete")
                || p.contains("terracotta") || p.contains("prismarine") || p.contains("calcite") || p.contains("quartz")
                || p.contains("purpur") || p.contains("copper")) {
            return (int) (150 * shape);
        }
        return (int) (100 * shape);
    }
}
