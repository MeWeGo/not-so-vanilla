package dev.grounded.survival;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Saro's Fruit Trees 1.0.4 gives its orange and pear a saturation modifier of 24 (vanilla apple: 0.3), i.e. 192
 * saturation per fruit. That makes them better than steak and pushes Spice of Life's diversity score to ~25.
 * Reset them to apple values: 4 nutrition, 2.4 saturation.
 */
final class FoodFixes {

    private FoodFixes() {
    }

    private static final String[] APPLE_LIKE = {"saros_fruit_trees_mod:orange", "saros_fruit_trees_mod:pear"};

    static void apply(ModifyDefaultComponentsEvent event) {
        for (String id : APPLE_LIKE) {
            BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).ifPresent(item ->
                    event.modify(item, builder -> builder.set(DataComponents.FOOD, new FoodProperties(4, 2.4F, false))));
        }
    }
}
