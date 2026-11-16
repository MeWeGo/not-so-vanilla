package dev.grounded.inventory.client.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import tnt.tarkovcraft.medsystem.client.MedicalSystemClient;
import tnt.tarkovcraft.medsystem.common.effect.StatusEffect;
import tnt.tarkovcraft.medsystem.common.effect.StatusEffectType;
import tnt.tarkovcraft.medsystem.common.effect.util.EffectVisibility;
import tnt.tarkovcraft.medsystem.common.init.MedSystemTags;
import tnt.tarkovcraft.medsystem.client.config.HealthDisplayType;
import tnt.tarkovcraft.medsystem.client.config.MedSystemClientConfig;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.common.health.Limb;
import tnt.tarkovcraft.medsystem.common.health.LimbContainer;
import tnt.tarkovcraft.medsystem.common.init.MedSystemDataAttachments;

import java.util.ArrayList;
import java.util.List;

/** Only class-loaded when Medical System is present. Reads the synced HEALTH_CONTAINER attachment. */
public final class MedCompat {

    public record LimbInfo(Component name, float health, float max, boolean dead) {
    }

    public record Health(float health, float max, List<LimbInfo> limbs, float displayScale) {
    }

    public static Health read(Player player) {
        if (!HealthSystem.hasCustomHealth(player)) return null;
        HealthContainer container = player.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        LimbContainer limbs = container.getLimbContainer();
        if (limbs.isEmpty()) return null;
        List<LimbInfo> list = new ArrayList<>();
        for (Limb limb : limbs) {
            list.add(new LimbInfo(limb.getDisplayName(), limb.getHealth(), limb.getMaxHealth(), limb.isDead()));
        }
        // same number scaling as Medical System's Health tab
        MedSystemClientConfig config = MedicalSystemClient.getConfig();
        float scale = config.healthDisplayType == HealthDisplayType.HEARTS ? 0.5F : (float) Math.pow(10, config.numericHealthScale);
        return new Health(limbs.getHealth(), limbs.getMaxHealth(), list, scale);
    }

    /** A visible Medical System status effect, tagged with the limb it sits on (null = root limb / whole body). */
    public record EffectInfo(Identifier icon, Component name, Component limb, Component duration, List<Component> tooltip) {
    }

    public static List<EffectInfo> effects(Player player) {
        List<EffectInfo> out = new ArrayList<>();
        if (!HealthSystem.hasCustomHealth(player)) return out;
        HealthContainer container = player.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        LimbContainer limbs = container.getLimbContainer();
        Limb root = limbs.getRootLimb();
        for (Limb limb : limbs) {
            boolean isRoot = limb == root;
            limb.getStatusEffects().getEffectsStream()
                    .filter(effect -> StatusEffectType.isVisible(effect, EffectVisibility.UI))
                    .forEach(effect -> out.add(describe(effect, isRoot ? null : limb.getDisplayName(), limb.getDisplayName(), isRoot)));
        }
        return out;
    }

    private static EffectInfo describe(StatusEffect effect, Component limbLabel, Component limbName, boolean root) {
        StatusEffectType<?> type = effect.getType();
        Component name = type.getDisplayName(effect).copy().withStyle(type.getContextualEffectType(effect));
        Component duration = effect.hasVisibleDuration() && !effect.isInfinite() ? StatusEffect.getDurationLabel(effect.getDuration()) : null;
        // same tooltip content as Medical System's Health tab
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(name);
        tooltip.add(root
                ? Component.translatable("label.grounded_inventory.effects.limb_root", limbName).withStyle(ChatFormatting.GRAY)
                : Component.translatable("label.grounded_inventory.effects.limb", limbName).withStyle(ChatFormatting.GRAY));
        effect.addAdditionalInfo(tooltip::add);
        if (duration != null) tooltip.add(duration);
        if (type.is(MedSystemTags.StatusEffects.IS_PAIN_CAUSING)) tooltip.add(StatusEffect.PAINFUL_LABEL);
        effect.addCustomTags(tooltip::add);
        return new EffectInfo(type.getIcon(effect), name, limbLabel, duration, tooltip);
    }

    public static String format(float value, float scale) {
        return String.valueOf(Mth.floor(value * scale));
    }

    private MedCompat() {
    }
}
