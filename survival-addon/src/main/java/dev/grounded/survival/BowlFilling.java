package dev.grounded.survival;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import dev.grounded.survival.item.WaterBowlItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-clicking water with a plain wooden bowl scoops one drink, graded by Thirst Was Taken 2's water sampling. */
final class BowlFilling {

    private BowlFilling() {
    }

    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (!held.is(Items.BOWL)) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
            return;
        }
        event.setCanceled(true);
        WaterQuality quality = WaterPurity.sampleAt(level, pos);
        if (!(quality instanceof WaterQuality.Fresh fresh)) {
            // A wooden bowl is no way to carry sea water; leave it to bottles and buckets.
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.grounded_survival.bowl_salt_water"));
            }
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 0.8F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        if (!level.isClientSide()) {
            ItemStack filled = WaterBowlItem.filled(new ItemStack(GroundedSurvival.WATER_BOWL.get()), fresh.purity());
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, filled));
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
