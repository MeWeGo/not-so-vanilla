package dev.grounded.inventory.client.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import team.creative.solonion.api.FoodPlayerData;
import team.creative.solonion.api.SOLOnionAPI;
import team.creative.solonion.client.gui.screen.FoodBookScreen;
import team.creative.solonion.common.SOLOnion;
import team.creative.solonion.common.benefit.BenefitThreshold;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Only class-loaded when Spice of Life: Onion is present. Diversity and the eaten-food queue come from
 * SOLOnionAPI's synced player data; benefit/detriment thresholds come from Onion's own config and are
 * judged active with the same rule its Food Book uses (benefit: threshold <= diversity, detriment: threshold > diversity).
 */
public final class OnionCompat {

    public record Diet(double diversity, double nextThreshold, double maxThreshold, int activeBenefits, int activeDetriments,
                       List<Component> tooltip) {
    }

    public static Diet read(Player player) {
        FoodPlayerData food = SOLOnionAPI.getFoodCapability(player);
        if (food == null) return null;
        double diversity = food.foodDiversity(player);

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("label.grounded_inventory.diet").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("label.grounded_inventory.diet.diversity", fmt(diversity)).withStyle(ChatFormatting.GRAY));

        int active = 0, activeBad = 0;
        double next = Double.NaN, max = 0;
        List<Component> good = new ArrayList<>();
        List<Component> bad = new ArrayList<>();
        if (SOLOnion.CONFIG != null) {
            for (BenefitThreshold t : sorted(SOLOnion.CONFIG.benefits)) {
                max = Math.max(max, t.threshold);
                if (t.threshold <= diversity) {
                    active++;
                    good.add(Component.literal("  + ").append(name(t)).withStyle(ChatFormatting.GREEN));
                } else if (Double.isNaN(next) || t.threshold < next) {
                    next = t.threshold;
                }
            }
            for (BenefitThreshold t : sorted(SOLOnion.CONFIG.detriments)) {
                if (t.threshold > diversity) {
                    activeBad++;
                    bad.add(Component.literal("  - ").append(name(t)).withStyle(ChatFormatting.RED));
                }
            }
        }
        if (!good.isEmpty()) {
            tooltip.add(Component.translatable("gui.solonion.food_book.active_benefits_header").withStyle(ChatFormatting.DARK_GREEN));
            tooltip.addAll(good);
        }
        if (!bad.isEmpty()) {
            tooltip.add(Component.translatable("gui.solonion.food_book.active_detriments_header").withStyle(ChatFormatting.DARK_RED));
            tooltip.addAll(bad);
        }
        if (!Double.isNaN(next)) {
            tooltip.add(Component.translatable("label.grounded_inventory.diet.next", fmt(next)).withStyle(ChatFormatting.GRAY));
        }

        // most recently eaten foods, as tracked by Onion
        List<Component> recent = new ArrayList<>();
        for (ItemStack stack : food) {
            if (!stack.isEmpty()) recent.add(Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (!recent.isEmpty()) {
            tooltip.add(Component.translatable("label.grounded_inventory.diet.recent", recent.size(), food.trackCount()).withStyle(ChatFormatting.YELLOW));
            int from = Math.max(0, recent.size() - 6);
            if (from > 0) tooltip.add(Component.literal("  …").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.addAll(recent.subList(from, recent.size()));
        }
        tooltip.add(Component.translatable("label.grounded_inventory.diet.click").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return new Diet(diversity, next, max, active, activeBad, tooltip);
    }

    /** Opens Onion's own Food Book screen (full benefit list). */
    public static void openFoodBook() {
        Player player = Minecraft.getInstance().player;
        if (player != null) FoodBookScreen.open(player);
    }

    private static List<BenefitThreshold> sorted(List<BenefitThreshold> list) {
        if (list == null) return List.of();
        List<BenefitThreshold> copy = new ArrayList<>(list);
        copy.sort(null);
        return copy;
    }

    /** Same naming as Onion's Food Book: effect name + roman level, or ±value attribute name. */
    private static MutableComponent name(BenefitThreshold t) {
        Object property = t.benefit.property.value;
        double value = t.benefit.value;
        if (property instanceof MobEffect effect) {
            return Component.translatable(effect.getDescriptionId()).append(" ")
                    .append(Component.translatable("enchantment.level." + ((int) value + 1)));
        }
        if (property instanceof Attribute attribute) {
            return Component.literal((value >= 0 ? "+" : "-") + fmt(Math.abs(value)) + " ")
                    .append(Component.translatable(attribute.getDescriptionId()));
        }
        return Component.literal(String.valueOf(property));
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }

    private OnionCompat() {
    }
}
