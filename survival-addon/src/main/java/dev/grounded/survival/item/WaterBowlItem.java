package dev.grounded.survival.item;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.WaterSickness;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A wooden bowl of water. It stores its purity in Thirst Was Taken 2's own {@code thirstwastaken2:water_purity}
 * component, so the mod's boiling grades apply: 0 dirty, 1 murky, 2 clean (boiled), 3 pure.
 * Drinking gives the same thirst as a plain water drink, scaled quenched by purity, and rolls the same sickness.
 */
public class WaterBowlItem extends Item {

    public WaterBowlItem(Item.Properties properties) {
        super(properties);
    }

    public static int purity(ItemStack stack) {
        Integer grade = ItemWaterData.grade(stack);
        return grade == null ? 0 : grade;
    }

    public static ItemStack filled(ItemStack bowlStack, int purity) {
        ItemWaterData.setFresh(bowlStack, purity);
        // Same model switch as Thirst Was Taken 2's terracotta bowl: index 1 = purity.
        bowlStack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(0.0F, (float) purity), List.of(), List.of(), List.of()));
        return bowlStack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!ThirstManager.canDrinkWater(player)) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            WaterQuality.Fresh quality = new WaterQuality.Fresh(purity(stack));
            int[] base = ThirstConfig.get().plainWaterValue;
            ThirstApi.drink(player, base[0], WaterPurity.quenched(quality, base[1]));
            WaterSickness.drink(player, quality);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(WaterPurity.tooltip(purity(stack)));
    }
}
