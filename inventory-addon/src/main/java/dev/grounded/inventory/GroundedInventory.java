package dev.grounded.inventory;

import dev.grounded.inventory.network.DragHealPayload;
import dev.grounded.inventory.server.DragHealServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;

/**
 * Common entry point. The inventory screen itself is client-only (it drives the vanilla player inventory
 * menu). The server side only exists for Tarkov-style drag-and-drop healing: an optional packet, so a
 * client with this mod can still join a server without it (drag-healing is then disabled).
 */
@Mod(GroundedInventory.MOD_ID)
public final class GroundedInventory {

    public static final String MOD_ID = "grounded_inventory";

    static {
        // Force US locale so mods calling toLowerCase() without Locale (e.g. TarkovCraft Notification$Severity)
        // do not turn "INFO" into Turkish dotless "ınfo" (\u0131).
        Locale.setDefault(Locale.US);
    }

    public GroundedInventory(IEventBus modEventBus) {
        Locale.setDefault(Locale.US);
        if (ModList.get().isLoaded("medsystem")) {
            modEventBus.addListener(GroundedInventory::registerPayloads);
            DragHealServer.registerEvents();
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(DragHealPayload.TYPE, DragHealPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> DragHealServer.handle(payload, context.player())));
    }
}
