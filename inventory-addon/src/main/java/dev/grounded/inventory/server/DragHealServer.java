package dev.grounded.inventory.server;

import dev.grounded.inventory.network.DragHealPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tnt.tarkovcraft.medsystem.api.heal.HealItemAttributes;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.common.health.Limb;
import tnt.tarkovcraft.medsystem.common.init.MedSystemItemComponents;
import tnt.tarkovcraft.medsystem.common.item.HealingItem;
import tnt.tarkovcraft.medsystem.common.item.InteractableItem;
import tnt.tarkovcraft.medsystem.common.item.InteractionTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of drag-and-drop healing. Only loaded when Medical System is installed.
 * <p>
 * It does not heal anything itself: it validates the drop with Medical System's own checks
 * ({@link HealItemAttributes#canUseOn} / {@link HealItemAttributes#canUseOnLimb}, the same ones its limb-select
 * packet uses), moves the cursor item into the main hand, stores the chosen limb as the item's
 * {@link InteractionTarget} and starts the normal vanilla item use. From then on Medical System's
 * {@code InteractableItem}/{@code HealingItem} code runs unchanged: use time, cycle healing, effect recovery,
 * item consumption and cooldowns.
 * <p>
 * Tarkov-style interruption (only for treatments started this way): taking combat/fall/explosion damage,
 * attacking or sprinting stops the use. Medical System then keeps whatever cycles already completed
 * (an interrupted bandage gives partial healing) and skips its finish step (no bleed/fracture removal).
 */
public final class DragHealServer {

    private record Session(String limbCode, long startTick) {
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(DragHealServer::onDamage);
        NeoForge.EVENT_BUS.addListener(DragHealServer::onAttack);
        NeoForge.EVENT_BUS.addListener(DragHealServer::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(DragHealServer::onLogout);
    }

    public static void handle(DragHealPayload payload, Player player) {
        if (!(player instanceof ServerPlayer sp) || !sp.isAlive() || sp.isSpectator()) return;
        String failure = tryStart(sp, payload.limbCode());
        if (failure != null) {
            sp.sendOverlayMessage(Component.translatable("message.grounded_inventory.heal." + failure).withStyle(ChatFormatting.RED));
            sp.inventoryMenu.sendAllDataToRemote(); // resync the client's cursor/slots
        }
    }

    /** @return null on success, otherwise a failure key */
    private static String tryStart(ServerPlayer player, String limbCode) {
        if (player.containerMenu != player.inventoryMenu) return "not_in_inventory";
        ItemStack carried = player.inventoryMenu.getCarried();
        if (carried.isEmpty() || !(carried.getItem() instanceof InteractableItem)) return "not_medical";
        if (player.isUsingItem()) return "busy";
        if (player.getCooldowns().isOnCooldown(carried)) return "cooldown";
        if (player.isCrouching()) return "crouching";
        if (!HealthSystem.hasCustomHealth(player)) return "no_health";
        HealthContainer container = HealthContainer.getAttached(player);
        if (!container.hasLimb(limbCode)) return "bad_limb";
        Limb limb = container.getLimbByCode(limbCode);

        HealItemAttributes attributes = carried.get(MedSystemItemComponents.HEAL_ATTRIBUTES);
        boolean limbTargeted = carried.getItem() instanceof HealingItem && attributes != null && !attributes.applyGlobally();
        if (attributes != null) {
            if (!attributes.canUseOn(player, player, carried, container)) return "nothing_to_treat";
            if (limbTargeted && !attributes.canUseOnLimb(limb, carried, container, player)) return "wrong_limb";
        }

        // Medical System items work from the main hand: put the cursor item there
        int selected = player.getInventory().getSelectedSlot();
        ItemStack previous = player.getInventory().getItem(selected);
        if (!previous.isEmpty()) {
            int free = player.getInventory().getFreeSlot();
            if (free < 0) return "no_space";
            player.getInventory().setItem(free, previous);
        }
        player.getInventory().setItem(selected, carried);
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges(); // client must see the item in hand before the "using" flag arrives
        ItemStack stack = player.getMainHandItem();
        if (carried.getItem() instanceof HealingItem) {
            // the same component Medical System's own limb-select screen writes
            stack.set(MedSystemItemComponents.INTERACTION_TARGET, new InteractionTarget(true, 0, limbCode));
        }
        player.gameMode.useItem(player, player.level(), stack, InteractionHand.MAIN_HAND);
        player.inventoryMenu.broadcastChanges();
        if (!player.isUsingItem()) {
            player.getMainHandItem().remove(MedSystemItemComponents.INTERACTION_TARGET);
            return "could_not_start";
        }
        SESSIONS.put(player.getUUID(), new Session(limbCode, player.level().getGameTime()));
        return null;
    }

    private static void interrupt(ServerPlayer player, String reason) {
        if (SESSIONS.remove(player.getUUID()) == null) return;
        if (!player.isUsingItem()) return;
        ItemStack using = player.getUseItem();
        player.stopUsingItem();
        using.remove(MedSystemItemComponents.INTERACTION_TARGET);
        player.getCooldowns().addCooldown(using, 10);
        player.sendOverlayMessage(Component.translatable("message.grounded_inventory.heal.interrupted",
                Component.translatable("message.grounded_inventory.heal.reason." + reason)).withStyle(ChatFormatting.RED));
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SESSIONS.containsKey(player.getUUID())) return;
        if (event.getOriginalDamage() <= 0) return;
        if (isInterruptingDamage(event.getSource())) interrupt(player, "damage");
    }

    /** Combat, falls and explosions interrupt; bleeding, hunger, thirst, freezing, poison etc. do not. */
    private static boolean isInterruptingDamage(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) interrupt(player, "attack");
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof InteractableItem)) {
            SESSIONS.remove(player.getUUID()); // finished, cancelled by Medical System, closed the screen or switched item
            return;
        }
        if (player.isSprinting()) interrupt(player, "sprint");
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private DragHealServer() {
    }
}
