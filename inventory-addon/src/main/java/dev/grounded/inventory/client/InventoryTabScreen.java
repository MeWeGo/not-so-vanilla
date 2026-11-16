package dev.grounded.inventory.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import tnt.tarkovcraft.core.client.screen.ColorPalette;
import tnt.tarkovcraft.core.client.screen.navigation.CoreNavigators;
import tnt.tarkovcraft.core.client.screen.navigation.NavigationEntry;
import tnt.tarkovcraft.core.client.screen.renderable.LabelRenderable;
import tnt.tarkovcraft.core.client.screen.renderable.VerticalLineRenderable;
import tnt.tarkovcraft.core.client.screen.widget.HorizontalNavigationMenu;
import tnt.tarkovcraft.core.client.screen.widget.LabelButton;
import tnt.tarkovcraft.core.common.attribute.AttributeSystem;
import tnt.tarkovcraft.core.common.init.CoreAttributes;
import tnt.tarkovcraft.core.common.weight.WeightSystem;

import java.util.UUID;

/**
 * "Inventory" tab of the TarkovCraft character screen.
 * <p>
 * Visually it copies {@code CharacterSubScreen} (blurred background, profile title, horizontal
 * tab navigation) so it sits next to the Statistics / Health / Skills tabs. It cannot extend
 * {@code CharacterSubScreen} because item interaction needs an {@link AbstractContainerScreen}.
 */
public class InventoryTabScreen extends AbstractContainerScreen<ProxyInventoryMenu> {

    // ---- layout (relative to leftPos/topPos), slot coords are the 16x16 item position ----
    static final int PANEL_W = 352;
    static final int PANEL_H = 196;
    static final int PITCH = 20;
    /** effects column to the right of the panel; part of imageWidth so JEI / click-outside respect it */
    static final int EFFECTS_GAP = 10, EFFECTS_W = 108;

    static final int ARMOR_X = 1, ARMOR_Y = 13, ARMOR_PITCH = 24;
    static final int MODEL_X0 = 24, MODEL_Y0 = 12, MODEL_X1 = 104, MODEL_Y1 = 103;
    static final int CRAFT_X = 111, CRAFT_Y = 13;
    static final int RESULT_X = 121, RESULT_Y = 60;
    static final int OFFHAND_X = 121, OFFHAND_Y = 85;
    static final int GRID_X = 171, GRID_Y = 13;
    static final int HOTBAR_Y = 87;
    static final int WEIGHT_Y = 112;
    static final int STATUS_Y = 123;
    static final int ARMOR_INFO_Y = 140;

    private static final int SLOT_BG = 0xAA101010;
    private static final int SLOT_BORDER = 0xFF4A4A44;
    private static final int SLOT_BORDER_EQUIP = 0xFF77705A;
    private static final int SECTION_BG = 0x33000000;
    private static final int LABEL_COLOR = ColorPalette.TEXT_COLOR;

    private final UUID profileId;
    private StatusPanel statusPanel;
    private EffectsPanel effectsPanel;
    private static final boolean MED = net.neoforged.fml.ModList.get().isLoaded("medsystem");
    /** drag-and-drop healing (Medical System only) */
    private dev.grounded.inventory.client.compat.@org.jspecify.annotations.Nullable MedDragHealClient dragHeal;
    private boolean swallowNextRelease;
    private boolean closingViaOnClose;

    public InventoryTabScreen(LocalPlayer player) {
        super(new ProxyInventoryMenu(player.inventoryMenu, InventoryTabScreen::layout), player.getInventory(),
                GroundedInventoryClient.INVENTORY.label(), PANEL_W + EFFECTS_GAP + EFFECTS_W, PANEL_H);
        this.profileId = player.getUUID();
    }

    /** Maps a vanilla InventoryMenu slot index to its position in this screen. */
    static ProxyInventoryMenu.Pos layout(int index) {
        if (index == InventoryMenu.RESULT_SLOT) {
            return new ProxyInventoryMenu.Pos(RESULT_X, RESULT_Y);
        }
        if (index >= InventoryMenu.CRAFT_SLOT_START && index < InventoryMenu.CRAFT_SLOT_END) {
            int i = index - InventoryMenu.CRAFT_SLOT_START;
            return new ProxyInventoryMenu.Pos(CRAFT_X + (i % 2) * PITCH, CRAFT_Y + (i / 2) * PITCH);
        }
        if (index >= InventoryMenu.ARMOR_SLOT_START && index < InventoryMenu.ARMOR_SLOT_END) {
            int i = index - InventoryMenu.ARMOR_SLOT_START; // head, chest, legs, feet
            return new ProxyInventoryMenu.Pos(ARMOR_X, ARMOR_Y + i * ARMOR_PITCH);
        }
        if (index >= InventoryMenu.INV_SLOT_START && index < InventoryMenu.INV_SLOT_END) {
            int i = index - InventoryMenu.INV_SLOT_START;
            return new ProxyInventoryMenu.Pos(GRID_X + (i % 9) * PITCH, GRID_Y + (i / 9) * PITCH);
        }
        if (index >= InventoryMenu.USE_ROW_SLOT_START && index < InventoryMenu.USE_ROW_SLOT_END) {
            int i = index - InventoryMenu.USE_ROW_SLOT_START;
            return new ProxyInventoryMenu.Pos(GRID_X + i * PITCH, HOTBAR_Y);
        }
        if (index == InventoryMenu.SHIELD_SLOT) {
            return new ProxyInventoryMenu.Pos(OFFHAND_X, OFFHAND_Y);
        }
        return null; // slots added by other mods to InventoryMenu: hidden, but indices stay aligned
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = Math.max(5, (this.width - this.imageWidth) / 2);
        this.topPos = 25 + Math.max(4, (this.height - 25 - this.imageHeight) / 2);
        this.statusPanel = new StatusPanel(this.font);
        this.effectsPanel = new EffectsPanel(this, this.font);
        if (MED && this.dragHeal == null) this.dragHeal = new dev.grounded.inventory.client.compat.MedDragHealClient();
        this.addHeader();
    }

    // ---- header copied from CharacterSubScreen so the tab bar looks and behaves the same ----

    private void addHeader() {
        PlayerInfo info = this.minecraft.getConnection().getPlayerInfo(this.profileId);
        String name = info != null ? info.getProfile().name() : this.minecraft.player.getName().getString();
        Component title = Component.translatable("label.tarkovcraft_core.profile", name);
        int titleWidth = this.font.width(title);
        LabelRenderable titleLabel = this.addRenderableOnly(LabelRenderable.fromComponent(5, 8, titleWidth, 10, this.font, title));
        titleLabel.setScrolling(false);
        this.addRenderableOnly(new VerticalLineRenderable(10 + titleWidth, 5, 19, ARGB.opaque(ColorPalette.TEXT_COLOR)));
        this.addRenderableWidget(new HorizontalNavigationMenu<>(15 + titleWidth, 0, this.width - 30 - titleWidth, 25, this,
                this.profileId, CoreNavigators.CHARACTER_NAVIGATION_PROVIDER, (entry, parent, userId) -> this.buildNavItem(entry)));
    }

    private AbstractWidget buildNavItem(NavigationEntry entry) {
        Component title = entry.label();
        int itemWidth = this.font.width(title);
        LabelButton button = new LabelButton(
                Button.builder(title, b -> this.minecraft.gui.setScreen(entry.getScreen(this, this.profileId)))
                        .size(itemWidth + 4, 15)
                        .pos(0, 5)
        );
        button.active = entry != GroundedInventoryClient.INVENTORY;
        button.setBackgroundHoverColor(0);
        button.setColorSelected(ColorPalette.WHITE);
        button.setColorDisabled(ColorPalette.YELLOW);
        button.setColor(ColorPalette.TEXT_COLOR);
        return button;
    }

    // ---- rendering ----

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // same backdrop as CharacterSubScreen + the weak body panel HealthScreen adds
        this.extractBlurredBackground(g);
        g.fill(0, 0, this.width, this.height, ColorPalette.BG_TRANSPARENT_NORMAL);
        g.fill(0, 0, this.width, 25, ColorPalette.BG_TRANSPARENT_WEAK);
        g.fill(0, 25, this.width, this.height, ColorPalette.BG_TRANSPARENT_WEAK);

        int x = this.leftPos;
        int y = this.topPos;
        // section backgrounds
        g.fill(x - 4, y - 4, x + 150, y + 108, SECTION_BG);
        g.fill(x + 166, y - 4, x + PANEL_W + 2, y + 108, SECTION_BG);
        g.fill(x + 166, y + WEIGHT_Y - 4, x + PANEL_W + 2, y + PANEL_H, SECTION_BG);
        g.fill(x - 4, y + WEIGHT_Y - 4, x + 150, y + PANEL_H, SECTION_BG);

        // character model in the middle of the equipment block
        g.fill(x + MODEL_X0, y + MODEL_Y0, x + MODEL_X1, y + MODEL_Y1, 0x55000000);
        g.outline(x + MODEL_X0, y + MODEL_Y0, MODEL_X1 - MODEL_X0, MODEL_Y1 - MODEL_Y0, SLOT_BORDER);
        if (this.minecraft.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g,
                    x + MODEL_X0 + 1, y + MODEL_Y0 + 1, x + MODEL_X1 - 1, y + MODEL_Y1 - 1,
                    40, 0.0625F, mouseX, mouseY, this.minecraft.player);
            if (this.dragHeal != null) {
                // limb silhouette over the model while a med item is on the cursor or a treatment runs
                this.dragHeal.extract(g, this.font, this.minecraft.player, this.menu.getCarried(),
                        x + MODEL_X0, y + MODEL_Y0, x + MODEL_X1, y + MODEL_Y1, mouseX, mouseY);
            }
        }

        // slot frames
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean equip = slot.index >= InventoryMenu.ARMOR_SLOT_START && slot.index < InventoryMenu.ARMOR_SLOT_END
                    || slot.index == InventoryMenu.SHIELD_SLOT;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_BG);
            g.outline(sx, sy, 18, 18, equip ? SLOT_BORDER_EQUIP : SLOT_BORDER);
        }
        // crafting arrow (2x2 grid -> result)
        g.centeredText(this.font, "↓", x + RESULT_X + 8, y + RESULT_Y - 12, LABEL_COLOR);

        this.extractWeight(g, x, y);
        if (this.minecraft.player != null) {
            this.statusPanel.extract(g, this.minecraft.player, x, y + STATUS_Y, mouseX, mouseY);
            this.extractArmorInfo(g, x, y);
            this.effectsPanel.extract(g, this.minecraft.player, x + PANEL_W + EFFECTS_GAP, y, EFFECTS_W, PANEL_H, mouseX, mouseY);
        }
    }

    private void extractArmorInfo(GuiGraphicsExtractor g, int x, int y) {
        LocalPlayer player = this.minecraft.player;
        int ax = x + GRID_X - 1;
        int ay = y + ARMOR_INFO_Y;
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, StatusPanel.ARMOR, ax, ay, 9, 9);
        double toughness = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        Component armor = Component.translatable("label.grounded_inventory.armor", player.getArmorValue(),
                String.format(java.util.Locale.ROOT, "%.0f", toughness));
        g.text(this.font, armor, ax + 12, ay + 1, LABEL_COLOR, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (this.hoveredSlot == null && this.dragHeal != null && this.dragHeal.getTooltip() != null) {
            g.setTooltipForNextFrame(this.font, this.dragHeal.getTooltip(), java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.hoveredSlot == null && this.menu.getCarried().isEmpty() && this.statusPanel != null) {
            java.util.List<Component> tip = this.statusPanel.getHoveredTooltip();
            if (tip == null && this.effectsPanel != null) tip = this.effectsPanel.getHoveredTooltip();
            if (tip != null) g.setTooltipForNextFrame(this.font, tip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void extractWeight(GuiGraphicsExtractor g, int x, int y) {
        int barX0 = x + GRID_X - 1;
        int barX1 = x + GRID_X - 1 + 9 * PITCH - 2;
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;
        if (!WeightSystem.isEnabled()) {
            g.text(this.font, Component.translatable("label.grounded_inventory.weight_disabled"), barX0, y + WEIGHT_Y, ColorPalette.TEXT_COLOR_DISABLED, false);
            return;
        }
        int weight = WeightSystem.getWeight(player);
        int limit = AttributeSystem.getIntValue(player, CoreAttributes.WEIGHT_LIMIT, 0);
        boolean overweight = WeightSystem.isOverweight(player);
        float factor = WeightSystem.getOverweightEffectFactor(player);
        int color = overweight ? (factor >= 1.0F ? 0xFFFF5555 : 0xFFFFFF55) : 0xFF55FF55;

        Component current = WeightSystem.getWeightValueDisplay(weight, WeightSystem.NO_STYLE);
        Component label = limit > 0
                ? Component.translatable("label.grounded_inventory.weight", current, WeightSystem.getWeightValueDisplay(limit, WeightSystem.NO_STYLE))
                : Component.translatable("label.grounded_inventory.weight_nolimit", current);
        g.text(this.font, label, barX0, y + WEIGHT_Y, color, false);
        if (overweight) {
            Component warn = Component.translatable("label.grounded_inventory.overweight");
            g.text(this.font, warn, barX1 - this.font.width(warn), y + WEIGHT_Y, color, false);
        }

        int barY = y + WEIGHT_Y + 11;
        g.fill(barX0, barY, barX1, barY + 6, 0xAA000000);
        g.outline(barX0, barY, barX1 - barX0, 6, SLOT_BORDER);
        if (limit > 0) {
            float ratio = Mth.clamp(weight / (float) limit, 0.0F, 1.0F);
            int fillX = barX0 + 1 + Mth.floor((barX1 - barX0 - 2) * ratio);
            g.fill(barX0 + 1, barY + 1, fillX, barY + 5, ARGB.color(0xCC, color));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // drawn translated to (leftPos, topPos)
        g.text(this.font, Component.translatable("label.grounded_inventory.equipment"), 0, 0, LABEL_COLOR, false);
        g.text(this.font, Component.translatable("label.grounded_inventory.crafting"), CRAFT_X - 1, 0, ColorPalette.TEXT_COLOR_DISABLED, false);
        g.text(this.font, Component.translatable("label.grounded_inventory.pockets"), GRID_X - 1, 0, LABEL_COLOR, false);
        g.text(this.font, Component.translatable("label.grounded_inventory.hotbar"), GRID_X - 1, HOTBAR_Y - 11, LABEL_COLOR, false);
        g.text(this.font, Component.translatable("label.grounded_inventory.status"), 0, STATUS_Y - 11, LABEL_COLOR, false);
        g.text(this.font, Component.translatable("label.grounded_inventory.offhand"), OFFHAND_X - 5, OFFHAND_Y - 10, ColorPalette.TEXT_COLOR_DISABLED, false);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        // click-to-drop: medical item already on the cursor, clicked on a limb
        if (this.dragHeal != null && this.dragHeal.isHoveringLimb()
                && dev.grounded.inventory.client.compat.MedDragHealClient.isMedicalItem(this.menu.getCarried())) {
            String limb = this.dragHeal.getDroppableLimb();
            if (limb != null) dev.grounded.inventory.client.compat.MedDragHealClient.sendDrop(limb);
            this.swallowNextRelease = true;
            return true;
        }
        if (event.button() == 0 && this.menu.getCarried().isEmpty() && this.statusPanel != null && this.statusPanel.isDietHovered()) {
            StatusPanel.openFoodBook();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (this.swallowNextRelease) {
            this.swallowNextRelease = false;
            return true;
        }
        // real drag: picked up from a slot, released over a limb
        if (this.dragHeal != null && this.dragHeal.isHoveringLimb()
                && dev.grounded.inventory.client.compat.MedDragHealClient.isMedicalItem(this.menu.getCarried())) {
            String limb = this.dragHeal.getDroppableLimb();
            if (limb != null) dev.grounded.inventory.client.compat.MedDragHealClient.sendDrop(limb);
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        // Clicks on empty space inside the panels must not throw the carried item;
        // only clicks clearly outside the inventory area (but below the tab bar) drop it.
        return my > 25 && (mx < xo - 6 || my < yo - 6 || mx >= xo + this.imageWidth + 4 || my >= yo + this.imageHeight + 2);
    }

    // ---- lifecycle ----

    @Override
    public void onClose() {
        this.closingViaOnClose = true;
        super.onClose(); // player.closeContainer(): sends the vanilla close packet
    }

    @Override
    public void removed() {
        super.removed();
        // Switched to another tab (or another screen replaced us) without onClose():
        // tell the server the inventory "closed" so a carried item / 2x2 crafting grid
        // goes back into the inventory, like closing the vanilla screen does.
        // Only when no other menu was opened meanwhile, so we never close a chest the
        // server just opened.
        LocalPlayer player = this.minecraft.player;
        if (!this.closingViaOnClose && player != null && player.containerMenu == player.inventoryMenu
                && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundContainerClosePacket(player.inventoryMenu.containerId));
        }
    }
}
