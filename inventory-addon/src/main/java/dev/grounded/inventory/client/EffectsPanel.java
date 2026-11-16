package dev.grounded.inventory.client;

import dev.grounded.inventory.client.compat.MedCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jspecify.annotations.Nullable;
import tnt.tarkovcraft.core.client.screen.ColorPalette;
import tnt.tarkovcraft.core.util.helper.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Active effects list, drawn to the right of the inventory panel (where vanilla puts its effect list).
 * Medical System status effects (bleeds, fractures, pain, painkillers...) come first, labelled with their limb,
 * then vanilla mob effects with level and remaining time.
 */
final class EffectsPanel {

    static final int ROW_H = 12;
    static final int ICON = 10;
    private static final boolean MED = ModList.get().isLoaded("medsystem");

    private interface Entry {
        void icon(GuiGraphicsExtractor g, int x, int y);

        Component name();

        @Nullable Component suffix();

        @Nullable Component duration();

        List<Component> tooltip();
    }

    private final AbstractContainerScreen<?> screen;
    private final Font font;
    private @Nullable List<Component> hoveredTooltip;

    EffectsPanel(AbstractContainerScreen<?> screen, Font font) {
        this.screen = screen;
        this.font = font;
    }

    @Nullable List<Component> getHoveredTooltip() {
        return this.hoveredTooltip;
    }

    private List<Entry> collect(Player player) {
        List<Entry> entries = new ArrayList<>();
        if (MED) {
            for (MedCompat.EffectInfo info : MedCompat.effects(player)) {
                entries.add(new Entry() {
                    public void icon(GuiGraphicsExtractor g, int x, int y) {
                        RenderUtils.blitFull(g, info.icon(), x, y, x + ICON, y + ICON);
                    }

                    public Component name() {
                        return info.name();
                    }

                    public @Nullable Component suffix() {
                        return info.limb();
                    }

                    public @Nullable Component duration() {
                        return info.duration();
                    }

                    public List<Component> tooltip() {
                        return info.tooltip();
                    }
                });
            }
        }
        float tickRate = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.tickRateManager().tickrate() : 20.0F;
        player.getActiveEffects().stream()
                .filter(ClientHooks::shouldRenderEffect)
                .sorted()
                .forEach(effect -> {
                    Component name = effectName(effect);
                    Component duration = MobEffectUtil.formatDuration(effect, 1.0F, tickRate);
                    entries.add(new Entry() {
                        public void icon(GuiGraphicsExtractor g, int x, int y) {
                            var renderer = IClientMobEffectExtensions.of(effect);
                            if (!renderer.extractInventoryIcon(effect, EffectsPanel.this.screen, g, x, y, ICON, ICON, CommonColors.WHITE)) {
                                g.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), x, y, ICON, ICON);
                            }
                        }

                        public Component name() {
                            return name;
                        }

                        public @Nullable Component suffix() {
                            return null;
                        }

                        public Component duration() {
                            return duration;
                        }

                        public List<Component> tooltip() {
                            List<Component> base = new ArrayList<>(List.of(name, duration.copy().withStyle(ChatFormatting.GRAY)));
                            return ClientHooks.getEffectTooltip(EffectsPanel.this.screen, effect, base);
                        }
                    });
                });
        return entries;
    }

    private static Component effectName(MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
            name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        } else if (effect.getAmplifier() > 9) {
            name.append(CommonComponents.SPACE).append(String.valueOf(effect.getAmplifier() + 1));
        }
        return name;
    }

    /**
     * @param x      left edge of the column
     * @param y      top of the column (label row)
     * @param width  available width
     * @param height available height (label included)
     */
    void extract(GuiGraphicsExtractor g, Player player, int x, int y, int width, int height, int mouseX, int mouseY) {
        this.hoveredTooltip = null;
        if (width < ICON + 4) return;
        List<Entry> entries = this.collect(player);
        boolean compact = width < 70; // icon-only column, details in the tooltip
        int colW = compact ? ICON + 6 : width;

        g.fill(x - 4, y - 4, x + colW, y + height, 0x33000000);
        g.text(this.font, Component.translatable(compact ? "label.grounded_inventory.effects.short" : "label.grounded_inventory.effects"),
                x, y, ColorPalette.TEXT_COLOR, false);
        int rowY = y + 11;
        if (entries.isEmpty()) {
            if (!compact) g.text(this.font, Component.translatable("label.grounded_inventory.effects.none"), x, rowY + 1, ColorPalette.TEXT_COLOR_DISABLED, false);
            return;
        }
        int maxRows = Math.max(1, (height - 11) / ROW_H);
        int shown = entries.size() > maxRows ? maxRows - 1 : entries.size();
        for (int i = 0; i < shown; i++) {
            Entry e = entries.get(i);
            int ry = rowY + i * ROW_H;
            g.fill(x - 1, ry - 1, x + ICON + 1, ry + ICON + 1, 0xAA101010);
            e.icon(g, x, ry);
            if (!compact) {
                int textX = x + ICON + 4;
                Component duration = e.duration();
                int durW = duration != null ? this.font.width(duration) : 0;
                int nameMax = colW - (ICON + 4) - (durW > 0 ? durW + 4 : 0) - 4;
                MutableComponent label = e.name().copy();
                if (e.suffix() != null) {
                    label.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                            .append(e.suffix().copy().withStyle(ChatFormatting.GRAY));
                }
                g.text(this.font, ComponentRenderUtils.clipText(label, this.font, Math.max(8, nameMax)), textX, ry + 1, CommonColors.WHITE, false);
                if (duration != null) {
                    g.text(this.font, duration, x + colW - 4 - durW, ry + 1, 0xFF9A9A9A, false);
                }
            }
            if (mouseX >= x - 1 && mouseX < x + colW - 2 && mouseY >= ry - 1 && mouseY < ry + ROW_H - 1) {
                this.hoveredTooltip = e.tooltip();
            }
        }
        if (shown < entries.size()) {
            int ry = rowY + shown * ROW_H;
            int rest = entries.size() - shown;
            g.text(this.font, "+" + rest, x, ry + 1, ColorPalette.TEXT_COLOR_DISABLED, false);
            if (mouseX >= x - 1 && mouseX < x + colW - 2 && mouseY >= ry - 1 && mouseY < ry + ROW_H - 1) {
                List<Component> tip = new ArrayList<>();
                for (int i = shown; i < entries.size(); i++) {
                    Entry e = entries.get(i);
                    MutableComponent line = e.name().copy();
                    if (e.suffix() != null) line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(e.suffix());
                    if (e.duration() != null) line.append(Component.literal("  ")).append(e.duration().copy().withStyle(ChatFormatting.GRAY));
                    tip.add(line);
                }
                this.hoveredTooltip = tip;
            }
        }
    }
}
