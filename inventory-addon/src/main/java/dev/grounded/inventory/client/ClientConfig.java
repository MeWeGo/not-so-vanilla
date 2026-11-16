package dev.grounded.inventory.client;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** config/grounded_inventory-client.toml (also editable in-game: Mods → Grounded Inventory → Config). */
public final class ClientConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_INVENTORY;
    public static final ModConfigSpec.BooleanValue HIDE_HUD_BARS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_HUD_LAYERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        REPLACE_VANILLA_INVENTORY = b
                .comment("Open the Tarkov-style Inventory tab instead of the vanilla inventory when pressing the inventory key (E).",
                        "Creative mode always keeps the vanilla creative inventory.")
                .define("replaceVanillaInventory", true);
        HIDE_HUD_BARS = b
                .comment("Hide the status bars from the bottom HUD (they are shown in the Inventory tab instead).",
                        "Note: with this on you can only check health/hunger/thirst/temperature by opening the inventory.")
                .define("hideHudBars", true);
        HIDDEN_HUD_LAYERS = b
                .comment("GUI layer ids hidden when hideHudBars = true.",
                        "Air, experience, hotbar, Medical System's limb overlay (top-left) and TarkovCraft stamina are kept by default.")
                .defineListAllowEmpty("hiddenHudLayers", List.of(
                        "minecraft:player_health",
                        "minecraft:food_level",
                        "minecraft:armor_level",
                        "thirstwastaken2:thirst_bar",
                        "cold_sweat:body_temp",
                        "cold_sweat:vague_temp"
                ), () -> "minecraft:player_health", o -> o instanceof String s && s.contains(":"));
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
