package dev.grounded.inventory.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Client-only "view" of the player's {@link InventoryMenu} with our own slot positions.
 * <p>
 * Every slot wraps the slot with the same index in {@code player.inventoryMenu}, and the menu
 * has container id 0. Clicks from the screen go through
 * {@code MultiPlayerGameMode.handleContainerInput(0, slotIndex, ...)}, which simulates the
 * click on {@code player.containerMenu} (the real InventoryMenu) and sends the normal vanilla
 * click packet. The server applies it to its own InventoryMenu, so behaviour (armor rules,
 * offhand, crafting, shift-click, drag-splitting, number keys) is identical to the vanilla
 * inventory screen and works in multiplayer without any custom packets.
 */
public final class ProxyInventoryMenu extends AbstractContainerMenu {

    public record Pos(int x, int y) {
    }

    private final InventoryMenu source;

    public ProxyInventoryMenu(InventoryMenu source, IntFunction<@Nullable Pos> layout) {
        super(null, source.containerId);
        this.source = source;
        for (Slot original : source.slots) {
            Pos pos = layout.apply(original.index);
            this.addSlot(new ProxySlot(original, pos));
        }
    }

    public InventoryMenu getSource() {
        return this.source;
    }

    @Override
    public ItemStack getCarried() {
        return this.source.getCarried();
    }

    @Override
    public void setCarried(ItemStack carried) {
        this.source.setCarried(carried);
    }

    @Override
    public int getStateId() {
        return this.source.getStateId();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return this.source.canTakeItemForPickAll(carried, unwrap(target));
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return this.source.canDragTo(unwrap(slot));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Never invoked: clicks are executed on the real InventoryMenu.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        // The real InventoryMenu owns all state; nothing to clean up here.
    }

    private static Slot unwrap(Slot slot) {
        return slot instanceof ProxySlot proxy ? proxy.original : slot;
    }

    /** Slot that mirrors another slot but sits at a different position. Hidden when pos is null. */
    static final class ProxySlot extends Slot {

        final Slot original;
        private final boolean visible;

        ProxySlot(Slot original, @Nullable Pos pos) {
            super(original.container, original.getSlotIndex(), pos != null ? pos.x() : -1000, pos != null ? pos.y() : -1000);
            this.original = original;
            this.visible = pos != null;
        }

        @Override
        public ItemStack getItem() {
            return this.original.getItem();
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return this.original.mayPlace(itemStack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.original.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return this.original.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack itemStack) {
            return this.original.getMaxStackSize(itemStack);
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return this.original.getNoItemIcon();
        }

        @Override
        public boolean isActive() {
            return this.visible && this.original.isActive();
        }

        @Override
        public boolean allowModification(Player player) {
            return this.original.allowModification(player);
        }

        @Override
        public boolean isHighlightable() {
            return this.original.isHighlightable();
        }

        @Override
        public boolean isFake() {
            return this.original.isFake();
        }
    }
}
