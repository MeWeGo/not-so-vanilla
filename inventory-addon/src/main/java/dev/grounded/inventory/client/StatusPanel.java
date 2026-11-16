package dev.grounded.inventory.client;

import dev.grounded.inventory.client.compat.ColdSweatCompat;
import dev.grounded.inventory.client.compat.MedCompat;
import dev.grounded.inventory.client.compat.OnionCompat;
import dev.grounded.inventory.client.compat.ThirstCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;
import tnt.tarkovcraft.core.api.StaminaComponent;
import tnt.tarkovcraft.core.client.screen.ColorPalette;
import tnt.tarkovcraft.core.common.energy.EnergySystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tarkov-meets-Minecraft status block: vanilla icons, thin Tarkov-style bars, numeric values.
 * Every value comes from the owning mod's API / synced data; integrations are optional.
 */
final class StatusPanel {

    static final int ROW_H = 12;
    static final int BAR_X = 12;
    static final int BAR_W = 86;
    static final int VALUE_X = 102;

    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier FOOD = Identifier.withDefaultNamespace("hud/food_full");
    static final Identifier ARMOR = Identifier.withDefaultNamespace("hud/armor_full");

    private static final boolean MED = ModList.get().isLoaded("medsystem");
    private static final boolean THIRST = ModList.get().isLoaded("thirstwastaken2");
    private static final boolean COLD_SWEAT = ModList.get().isLoaded("cold_sweat");
    private static final boolean ONION = ModList.get().isLoaded("solonion");

    private final Font font;
    private @Nullable List<Component> hoveredTooltip;
    private boolean dietHovered;
    private boolean onionBroken;

    StatusPanel(Font font) {
        this.font = font;
    }

    /** true while the mouse is over the Diet row (click opens Onion's Food Book) */
    boolean isDietHovered() {
        return this.dietHovered;
    }

    static void openFoodBook() {
        if (ONION) OnionCompat.openFoodBook();
    }

    @Nullable List<Component> getHoveredTooltip() {
        return this.hoveredTooltip;
    }

    /** Draws all rows at absolute (x, y); returns used height. */
    int extract(GuiGraphicsExtractor g, Player player, int x, int y, int mouseX, int mouseY) {
        this.hoveredTooltip = null;
        this.dietHovered = false;
        int row = 0;
        row += this.health(g, player, x, y + row * ROW_H, mouseX, mouseY);
        row += this.food(g, player, x, y + row * ROW_H, mouseX, mouseY);
        if (ONION && !this.onionBroken) row += this.diet(g, player, x, y + row * ROW_H, mouseX, mouseY);
        if (THIRST) row += this.thirst(g, player, x, y + row * ROW_H, mouseX, mouseY);
        if (COLD_SWEAT) row += this.temperature(g, player, x, y + row * ROW_H, mouseX, mouseY);
        row += this.stamina(g, player, x, y + row * ROW_H, mouseX, mouseY);
        return row * ROW_H;
    }

    // ---- rows ----

    private int health(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, x + 1, y, 9, 9);
        MedCompat.Health med = MED ? MedCompat.read(player) : null;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("label.grounded_inventory.status.health").withStyle(ChatFormatting.WHITE));
        if (med != null) {
            // one segment per limb, like Tarkov's body-part bars
            int n = med.limbs().size();
            int gap = 1;
            int segW = (BAR_W - gap * (n - 1)) / n;
            this.barBackground(g, x + BAR_X, y + 2, BAR_W);
            for (int i = 0; i < n; i++) {
                MedCompat.LimbInfo limb = med.limbs().get(i);
                float pct = limb.max() > 0 ? limb.health() / limb.max() : 0;
                int sx = x + BAR_X + i * (segW + gap);
                int color = limb.dead() ? 0xFF3A0000 : healthColor(pct);
                g.fill(sx + 1, y + 3, sx + 1 + Math.max(limb.dead() ? segW - 2 : 0, Mth.floor((segW - 2) * pct)), y + 6, color);
                tooltip.add(Component.empty()
                        .append(limb.name().copy().withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(": "))
                        .append(limb.dead()
                                ? Component.translatable("label.grounded_inventory.status.limb_destroyed").withStyle(ChatFormatting.DARK_RED)
                                : Component.literal(MedCompat.format(limb.health(), med.displayScale()) + "/" + MedCompat.format(limb.max(), med.displayScale()))
                                .withColor(healthColor(pct) & 0xFFFFFF)));
            }
            float pct = med.max() > 0 ? med.health() / med.max() : 0;
            this.value(g, x, y, MedCompat.format(med.health(), med.displayScale()) + "/" + MedCompat.format(med.max(), med.displayScale()), healthColor(pct));
        } else {
            float pct = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 0;
            this.bar(g, x + BAR_X, y + 2, BAR_W, pct, healthColor(pct));
            this.value(g, x, y, Mth.ceil(player.getHealth()) + "/" + Mth.ceil(player.getMaxHealth()), healthColor(pct));
        }
        this.hover(x, y, mx, my, tooltip);
        return 1;
    }

    private int food(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD, x + 1, y, 9, 9);
        FoodData food = player.getFoodData();
        int level = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        float pct = level / 20.0F;
        this.bar(g, x + BAR_X, y + 2, BAR_W, pct, pct <= 0.3F ? 0xFFCC4422 : 0xFFC08A3E);
        this.overlay(g, x + BAR_X, y + 2, BAR_W, Mth.clamp(saturation / 20.0F, 0, 1), 0xFFFFE070);
        this.value(g, x, y, level + "/20", pct <= 0.3F ? 0xFFFF7755 : ColorPalette.TEXT_COLOR);
        this.hover(x, y, mx, my, List.of(
                Component.translatable("label.grounded_inventory.status.hunger").withStyle(ChatFormatting.WHITE),
                Component.translatable("label.grounded_inventory.status.saturation", String.format(Locale.ROOT, "%.1f", saturation)).withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private int diet(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        OnionCompat.Diet diet;
        try {
            diet = OnionCompat.read(player);
        } catch (RuntimeException | LinkageError e) {
            this.onionBroken = true; // Onion internals changed: drop the row instead of crashing the screen
            return 0;
        }
        if (diet == null) return 0;
        drawCarrot(g, x + 1, y);
        double max = diet.maxThreshold() > 0 ? diet.maxThreshold() : Math.max(1.0, diet.diversity());
        int color = diet.activeDetriments() > 0 ? 0xFFCC4422 : diet.activeBenefits() > 0 ? 0xFF55CC55 : 0xFFB0A060;
        this.bar(g, x + BAR_X, y + 2, BAR_W, (float) (diet.diversity() / max), color);
        // threshold ticks of the next benefit
        if (!Double.isNaN(diet.nextThreshold()) && diet.maxThreshold() > 0) {
            int tx = x + BAR_X + 1 + Mth.floor((BAR_W - 2) * (float) (diet.nextThreshold() / max));
            g.fill(tx, y + 1, tx + 1, y + 8, 0xFFEEEEEE);
        }
        String text = (diet.diversity() == Math.rint(diet.diversity()) ? String.valueOf((long) diet.diversity())
                : String.format(Locale.ROOT, "%.1f", diet.diversity()));
        if (diet.activeBenefits() > 0) text += " +" + diet.activeBenefits();
        this.value(g, x, y, text, diet.activeDetriments() > 0 ? 0xFFFF7755 : ColorPalette.TEXT_COLOR);
        if (mx >= x && mx < x + VALUE_X + 48 && my >= y - 1 && my < y + ROW_H - 1) {
            this.hoveredTooltip = diet.tooltip();
            this.dietHovered = true;
        }
        return 1;
    }

    private static void drawCarrot(GuiGraphicsExtractor g, int x, int y) {
        int leaf = 0xFF4CAF50, body = 0xFFF08A24;
        g.fill(x + 5, y, x + 6, y + 2, leaf);
        g.fill(x + 7, y, x + 8, y + 2, leaf);
        g.fill(x + 6, y + 1, x + 7, y + 3, leaf);
        g.fill(x + 4, y + 3, x + 8, y + 5, body);
        g.fill(x + 3, y + 5, x + 6, y + 7, body);
        g.fill(x + 2, y + 7, x + 4, y + 9, body);
    }

    private int thirst(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        int[] t = ThirstCompat.read(player);
        if (t == null) return 0;
        drawDroplet(g, x + 2, y);
        float pct = t[2] > 0 ? t[0] / (float) t[2] : 0;
        this.bar(g, x + BAR_X, y + 2, BAR_W, pct, pct <= 0.3F ? 0xFFCC4422 : 0xFF3F8FE0);
        this.overlay(g, x + BAR_X, y + 2, BAR_W, t[2] > 0 ? Mth.clamp(t[1] / (float) t[2], 0, 1) : 0, 0xFFA8E4FF);
        this.value(g, x, y, t[0] + "/" + t[2], pct <= 0.3F ? 0xFFFF7755 : ColorPalette.TEXT_COLOR);
        this.hover(x, y, mx, my, List.of(
                Component.translatable("label.grounded_inventory.status.thirst").withStyle(ChatFormatting.WHITE),
                Component.translatable("label.grounded_inventory.status.quenched", t[1]).withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private int temperature(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        double temp = ColdSweatCompat.bodyTemperature(player);
        drawThermometer(g, x + 3, y);
        int bx = x + BAR_X;
        this.barBackground(g, bx, y + 2, BAR_W);
        int center = bx + BAR_W / 2;
        float frac = (float) Mth.clamp(temp / 150.0, -1.0, 1.0);
        int len = Mth.floor((BAR_W / 2 - 1) * Math.abs(frac));
        int color = tempColor(temp);
        if (frac >= 0) g.fill(center, y + 3, center + len, y + 6, color);
        else g.fill(center - len, y + 3, center, y + 6, color);
        // danger marks at ±100
        int mark = Mth.floor((BAR_W / 2 - 1) * (100 / 150.0F));
        g.fill(center - mark, y + 2, center - mark + 1, y + 7, 0xFF5577FF);
        g.fill(center + mark, y + 2, center + mark + 1, y + 7, 0xFFFF5533);
        g.fill(center, y + 2, center + 1, y + 7, 0xFF888888);
        this.value(g, x, y, String.valueOf((int) Math.round(temp)), color);
        String state = temp <= -100 ? "freezing" : temp <= -50 ? "cold" : temp >= 100 ? "overheating" : temp >= 50 ? "hot" : "normal";
        this.hover(x, y, mx, my, List.of(
                Component.translatable("label.grounded_inventory.status.temperature").withStyle(ChatFormatting.WHITE),
                Component.translatable("label.grounded_inventory.status.temperature." + state).withColor(color & 0xFFFFFF)));
        return 1;
    }

    private int stamina(GuiGraphicsExtractor g, Player player, int x, int y, int mx, int my) {
        StaminaComponent component = EnergySystem.MOVEMENT_STAMINA.getComponent();
        float max = component.getMaxStamina(player);
        if (max <= 0) return 0;
        float stamina = component.getStamina(player);
        float pct = Mth.clamp(stamina / max, 0, 1);
        boolean critical = component.isCriticalValue(player);
        drawBolt(g, x + 2, y);
        this.bar(g, x + BAR_X, y + 2, BAR_W, pct, critical ? 0xFFCC4422 : 0xFF8BC34A);
        this.value(g, x, y, Mth.floor(pct * 100) + "%", critical ? 0xFFFF7755 : ColorPalette.TEXT_COLOR);
        this.hover(x, y, mx, my, List.of(Component.translatable("label.grounded_inventory.status.stamina").withStyle(ChatFormatting.WHITE)));
        return 1;
    }

    // ---- drawing helpers ----

    private void barBackground(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 5, 0xCC000000);
        g.outline(x, y, w, 5, 0xFF4A4A44);
    }

    private void bar(GuiGraphicsExtractor g, int x, int y, int w, float pct, int color) {
        this.barBackground(g, x, y, w);
        int fill = Mth.floor((w - 2) * Mth.clamp(pct, 0, 1));
        if (fill > 0) g.fill(x + 1, y + 1, x + 1 + fill, y + 4, color);
    }

    /** thin line on the bottom edge of a bar (saturation / quenched) */
    private void overlay(GuiGraphicsExtractor g, int x, int y, int w, float pct, int color) {
        int fill = Mth.floor((w - 2) * Mth.clamp(pct, 0, 1));
        if (fill > 0) g.fill(x + 1, y + 3, x + 1 + fill, y + 4, color);
    }

    private void value(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        g.text(this.font, text, x + VALUE_X, y + 1, color, false);
    }

    private void hover(int x, int y, int mx, int my, List<Component> tooltip) {
        if (mx >= x && mx < x + VALUE_X + 48 && my >= y - 1 && my < y + ROW_H - 1) {
            this.hoveredTooltip = tooltip;
        }
    }

    static int healthColor(float pct) {
        if (pct > 0.75F) return 0xFF55CC55;
        if (pct > 0.4F) return 0xFFDDCC33;
        if (pct > 0.15F) return 0xFFE07B22;
        return 0xFFDD2222;
    }

    static int tempColor(double t) {
        if (t <= -100) return 0xFF3355FF;
        if (t <= -50) return 0xFF66BBFF;
        if (t >= 100) return 0xFFFF3322;
        if (t >= 50) return 0xFFFF9933;
        return ColorPalette.TEXT_COLOR;
    }

    private static void drawDroplet(GuiGraphicsExtractor g, int x, int y) {
        int c = 0xFF3F8FE0, h = 0xFFA8E4FF;
        g.fill(x + 3, y, x + 4, y + 2, c);
        g.fill(x + 2, y + 2, x + 5, y + 4, c);
        g.fill(x + 1, y + 4, x + 6, y + 8, c);
        g.fill(x + 2, y + 8, x + 5, y + 9, c);
        g.fill(x + 2, y + 5, x + 3, y + 7, h);
    }

    private static void drawThermometer(GuiGraphicsExtractor g, int x, int y) {
        int o = 0xFFDDDDDD, r = 0xFFE04030;
        g.fill(x + 1, y, x + 4, y + 6, o);
        g.fill(x + 2, y + 2, x + 3, y + 6, r);
        g.fill(x, y + 6, x + 5, y + 9, r);
    }

    private static void drawBolt(GuiGraphicsExtractor g, int x, int y) {
        int c = 0xFFFFD84A;
        g.fill(x + 3, y, x + 6, y + 1, c);
        g.fill(x + 2, y + 1, x + 5, y + 3, c);
        g.fill(x + 1, y + 3, x + 6, y + 5, c);
        g.fill(x + 2, y + 5, x + 4, y + 7, c);
        g.fill(x + 1, y + 7, x + 3, y + 9, c);
    }
}
