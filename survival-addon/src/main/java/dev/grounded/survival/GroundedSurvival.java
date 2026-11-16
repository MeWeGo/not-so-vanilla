package dev.grounded.survival;

import dev.grounded.survival.item.WaterBowlItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GroundedSurvival.MOD_ID)
public final class GroundedSurvival {

    public static final String MOD_ID = "grounded_survival";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    /** A plain wooden bowl dipped in water. Holds one drink; purity works like Thirst Was Taken 2's own vessels. */
    public static final DeferredItem<WaterBowlItem> WATER_BOWL = ITEMS.registerItem("water_bowl", WaterBowlItem::new,
            props -> props.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                    .usingConvertsTo(Items.BOWL));

    /** Squeezed fruit. Thirst values come from data/grounded_survival/thirstwastaken2/drinks/ (low on purpose). */
    public static final DeferredItem<Item> FRUIT_JUICE = ITEMS.registerSimpleItem("fruit_juice",
            props -> props.stacksTo(16).component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                    .usingConvertsTo(Items.GLASS_BOTTLE).craftRemainder(Items.GLASS_BOTTLE));

    public GroundedSurvival(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModifyDefaultComponentsEvent.class, ItemWeights::assign);
        modBus.addListener(ModifyDefaultComponentsEvent.class, FoodFixes::apply);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, GroundedSurvival::fillTabs);
        NeoForge.EVENT_BUS.addListener(BowlFilling::onRightClickItem);
    }

    private static void fillTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(WATER_BOWL.get());
            event.accept(FRUIT_JUICE.get());
        }
    }
}
