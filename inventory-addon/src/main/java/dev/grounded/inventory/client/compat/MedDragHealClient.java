package dev.grounded.inventory.client.compat;

import dev.grounded.inventory.network.DragHealPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import tnt.tarkovcraft.medsystem.api.heal.EffectRecovery;
import tnt.tarkovcraft.medsystem.api.heal.HealItemAttributes;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthContainerDisplay;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.common.health.Limb;
import tnt.tarkovcraft.medsystem.common.init.MedSystemItemComponents;
import tnt.tarkovcraft.medsystem.common.item.HealingItem;
import tnt.tarkovcraft.medsystem.common.item.InteractableItem;
import tnt.tarkovcraft.medsystem.common.item.InteractionTarget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client half of drag-and-drop healing (only loaded with Medical System). Draws the limb silhouette
 * over the character model while a medical item is on the cursor or a treatment runs, evaluates each limb
 * with Medical System's own checks to explain why a drop is (in)valid, and sends the drop to the server.
 */
public final class MedDragHealClient {

    public record Validity(boolean valid, List<Component> lines) {
    }

    private record LimbRect(String code, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private @Nullable String hoveredLimb;
    private @Nullable Validity hoveredValidity;
    private @Nullable List<Component> tooltip;
    // "treatment stopped" feedback
    private boolean wasTreating;
    private boolean lastTreatmentFinished;
    private long stoppedAt = -1;
    private int lastRemaining;

    public static boolean isMedicalItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof InteractableItem;
    }

    public static boolean serverSupportsDragHeal() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(DragHealPayload.TYPE.id());
    }

    public @Nullable List<Component> getTooltip() {
        return this.tooltip;
    }

    /** The limb under the mouse in the last frame, if dropping there is allowed. */
    public @Nullable String getDroppableLimb() {
        return this.hoveredValidity != null && this.hoveredValidity.valid() ? this.hoveredLimb : null;
    }

    public boolean isHoveringLimb() {
        return this.hoveredLimb != null;
    }

    public static void sendDrop(String limbCode) {
        ClientPacketDistributor.sendToServer(new DragHealPayload(limbCode));
    }

    /**
     * @return true when the silhouette was drawn (the screen should then hide its own model highlight)
     */
    public boolean extract(GuiGraphicsExtractor g, Font font, Player player, ItemStack carried,
                           int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        this.hoveredLimb = null;
        this.hoveredValidity = null;
        this.tooltip = null;
        if (!HealthSystem.hasCustomHealth(player)) return false;

        boolean treating = player.isUsingItem() && isMedicalItem(player.getUseItem());
        long now = player.level().getGameTime();
        if (this.wasTreating && !treating) {
            this.stoppedAt = now;
            // Medical System finishes (or ends a full heal) by running the use timer down to 0; an interruption stops early
            this.lastTreatmentFinished = this.lastRemaining <= 1;
        }
        this.wasTreating = treating;
        if (treating) this.lastRemaining = player.getUseItemRemainingTicks();
        boolean dragging = isMedicalItem(carried);
        boolean recentlyStopped = this.stoppedAt >= 0 && now - this.stoppedAt < 40;
        if (!dragging && !treating && !recentlyStopped) return false;

        HealthContainer container = HealthContainer.getAttached(player);
        HealthContainerDisplay display = container.getDefinition().display();
        List<LimbRect> rects = layout(display, x0 + 3, y0 + 3, x1 - 3, y1 - 14);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xCC0A0A0A);

        String treatedLimb = null;
        if (treating) {
            InteractionTarget target = player.getUseItem().get(MedSystemItemComponents.INTERACTION_TARGET);
            treatedLimb = target != null ? target.limbCode() : null;
        }
        boolean canDrop = serverSupportsDragHeal();
        for (LimbRect r : rects) {
            if (!container.hasLimb(r.code())) continue;
            Limb limb = container.getLimbByCode(r.code());
            float pct = limb.getMaxHealth() > 0 ? limb.getHealth() / limb.getMaxHealth() : 0;
            int color = limb.isDead() ? 0xFF2A0000 : (0xFF000000 | (healthColor(pct) & 0xFFFFFF));
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), (color & 0xFFFFFF) | 0x99000000);
            g.outline(r.x(), r.y(), r.w(), r.h(), 0xFF202020);
            boolean hovered = r.contains(mouseX, mouseY);
            if (dragging) {
                Validity v = canDrop ? evaluate(player, carried, container, limb) : new Validity(false,
                        List.of(Component.translatable("label.grounded_inventory.heal.no_server").withStyle(ChatFormatting.RED)));
                if (hovered) {
                    this.hoveredLimb = r.code();
                    this.hoveredValidity = v;
                    List<Component> tip = new ArrayList<>();
                    tip.add(carried.getHoverName().copy().append(Component.literal(" → ")).append(limb.getDisplayName()).withStyle(ChatFormatting.WHITE));
                    tip.addAll(v.lines());
                    this.tooltip = tip;
                    g.outline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, v.valid() ? 0xFFFFFFFF : 0xFFFF3030);
                } else if (v.valid()) {
                    g.outline(r.x(), r.y(), r.w(), r.h(), 0xFF9BE39B); // valid target hint
                }
            } else if (hovered) {
                this.tooltip = List.of(limb.getDisplayName().copy().withStyle(ChatFormatting.WHITE),
                        Component.literal(Mth.ceil(limb.getHealth()) + "/" + Mth.ceil(limb.getMaxHealth())).withStyle(ChatFormatting.GRAY));
            }
            if (r.code().equals(treatedLimb)) {
                g.outline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, 0xFFFFD84A);
            }
        }

        // bottom line: progress bar / hint
        int barY = y1 - 11;
        if (treating) {
            ItemStack use = player.getUseItem();
            int total = use.getUseDuration(player);
            int elapsed = player.getTicksUsingItem();
            boolean open = total >= 36000 /* Item.APPROXIMATELY_INFINITE_USE_DURATION (72000) / 2 */;
            float progress = open ? (elapsed % 40) / 40.0F : Mth.clamp(elapsed / (float) Math.max(1, total), 0, 1);
            g.fill(x0 + 2, barY, x1 - 2, barY + 5, 0xCC000000);
            g.outline(x0 + 2, barY, x1 - x0 - 4, 5, 0xFF4A4A44);
            g.fill(x0 + 3, barY + 1, x0 + 3 + Mth.floor((x1 - x0 - 6) * progress), barY + 4, 0xFFFFD84A);
            String secs = open ? String.format(java.util.Locale.ROOT, "%.1fs", elapsed / 20.0F)
                    : String.format(java.util.Locale.ROOT, "%.1fs", Math.max(0, total - elapsed) / 20.0F);
            g.centeredText(font, secs, (x0 + x1) / 2, barY - 10, 0xFFFFD84A);
            if (mouseX >= x0 && mouseX < x1 && mouseY >= barY - 11 && mouseY < y1 && this.tooltip == null) {
                this.tooltip = List.of(
                        use.getHoverName().copy().withStyle(ChatFormatting.WHITE),
                        Component.translatable("label.grounded_inventory.heal.treating").withStyle(ChatFormatting.YELLOW),
                        Component.translatable("label.grounded_inventory.heal.cancel_hint").withStyle(ChatFormatting.GRAY));
            }
        } else if (recentlyStopped && !dragging) {
            Component text = this.lastTreatmentFinished
                    ? Component.translatable("label.grounded_inventory.heal.done").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("label.grounded_inventory.heal.stopped").withStyle(ChatFormatting.RED);
            g.centeredText(font, text, (x0 + x1) / 2, barY - 2, 0xFFFFFFFF);
        } else if (dragging) {
            g.centeredText(font, Component.translatable("label.grounded_inventory.heal.drop_hint"), (x0 + x1) / 2, barY - 2, 0xFFB0B0B0);
        }
        return true;
    }

    /** Same checks the server makes (Medical System's own API), plus readable reasons. */
    private static Validity evaluate(Player player, ItemStack stack, HealthContainer container, Limb limb) {
        List<Component> lines = new ArrayList<>();
        if (player.isUsingItem()) return invalid(lines, "busy");
        if (player.getCooldowns().isOnCooldown(stack)) return invalid(lines, "cooldown");
        if (player.isCrouching()) return invalid(lines, "crouching");
        HealItemAttributes attributes = stack.get(MedSystemItemComponents.HEAL_ATTRIBUTES);
        if (attributes == null) {
            lines.add(Component.translatable("label.grounded_inventory.heal.whole_body").withStyle(ChatFormatting.GREEN));
            return new Validity(true, lines);
        }
        boolean limbTargeted = stack.getItem() instanceof HealingItem && !attributes.applyGlobally();
        if (!attributes.canUseOn(player, player, stack, container)) {
            invalid(lines, "nothing_to_treat");
            addUses(lines, attributes);
            return new Validity(false, lines);
        }
        if (!limbTargeted) {
            lines.add(Component.translatable("label.grounded_inventory.heal.whole_body").withStyle(ChatFormatting.GREEN));
            return new Validity(true, lines);
        }
        if (attributes.canUseOnLimb(limb, stack, container, player)) {
            lines.add(Component.translatable("label.grounded_inventory.heal.ok").withStyle(ChatFormatting.GREEN));
            for (EffectRecovery recovery : attributes.recoveries()) {
                if (recovery.canRecover(container, player, limb)) {
                    recovery.applicator().addLabels(l -> lines.add(Component.literal(" ").append(l).withStyle(ChatFormatting.GRAY)), n -> {});
                }
            }
            if (attributes.isSurgeryItem() && limb.isDead()) lines.add(Component.translatable("label.grounded_inventory.heal.surgery").withStyle(ChatFormatting.GRAY));
            return new Validity(true, lines);
        }
        // why not
        if (limb.isDead() && !attributes.isSurgeryItem()) {
            invalid(lines, "destroyed");
        } else if (attributes.isSurgeryItem() && !limb.isDead() && attributes.recoveries().isEmpty() && !attributes.isHealing()) {
            invalid(lines, "surgery_only");
        } else if (attributes.isHealing() && attributes.recoveries().isEmpty() && limb.getHealth() >= limb.getMaxHealth()) {
            invalid(lines, "full_health");
        } else {
            invalid(lines, "wrong_limb");
        }
        addUses(lines, attributes);
        return new Validity(false, lines);
    }

    private static Validity invalid(List<Component> lines, String key) {
        lines.add(Component.translatable("message.grounded_inventory.heal." + key).withStyle(ChatFormatting.RED));
        return new Validity(false, lines);
    }

    /** "This item treats: ..." using Medical System's own recovery labels (they include limb restrictions). */
    private static void addUses(List<Component> lines, HealItemAttributes attributes) {
        List<Component> uses = new ArrayList<>();
        for (EffectRecovery recovery : attributes.recoveries()) {
            recovery.applicator().addLabels(uses::add, uses::add);
        }
        if (attributes.isHealing()) uses.add(Component.translatable("label.grounded_inventory.heal.restores_hp"));
        if (attributes.isSurgeryItem()) uses.add(Component.translatable("label.grounded_inventory.heal.surgery"));
        if (uses.isEmpty()) return;
        lines.add(Component.translatable("label.grounded_inventory.heal.item_treats").withStyle(ChatFormatting.DARK_GRAY));
        uses.forEach(u -> lines.add(Component.literal(" ").append(u).withStyle(ChatFormatting.GRAY)));
    }

    /** Fits Medical System's limb display layout into the box. */
    private static List<LimbRect> layout(HealthContainerDisplay display, int bx0, int by0, int bx1, int by1) {
        Map<String, HealthContainerDisplay.DisplayData> data = new LinkedHashMap<>();
        display.accept(data::put);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector2f zero = new Vector2f();
        for (HealthContainerDisplay.DisplayData d : data.values()) {
            Vector4f p = d.getPos(1.0F, zero);
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x + p.z);
            maxY = Math.max(maxY, p.y + p.w);
        }
        List<LimbRect> out = new ArrayList<>();
        if (data.isEmpty() || maxX <= minX || maxY <= minY) return out;
        float scale = Math.min((bx1 - bx0) / (maxX - minX), (by1 - by0) / (maxY - minY));
        Vector2f center = new Vector2f((bx0 + bx1) / 2.0F - scale * (minX + maxX) / 2.0F, (by0 + by1) / 2.0F - scale * (minY + maxY) / 2.0F);
        for (Map.Entry<String, HealthContainerDisplay.DisplayData> e : data.entrySet()) {
            var p = e.getValue().getGuiPos(scale, center);
            out.add(new LimbRect(e.getKey(), p.x, p.y, Math.max(2, p.z), Math.max(2, p.w)));
        }
        return out;
    }

    private static int healthColor(float pct) {
        if (pct > 0.75F) return 0xFF55CC55;
        if (pct > 0.4F) return 0xFFDDCC33;
        if (pct > 0.15F) return 0xFFE07B22;
        return 0xFFDD2222;
    }
}
