package dev.grounded.inventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.grounded.inventory.GroundedInventory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import tnt.tarkovcraft.core.client.TarkovCraftCoreClient;
import tnt.tarkovcraft.core.client.screen.navigation.CoreNavigators;
import tnt.tarkovcraft.core.client.screen.navigation.NavigationEntry;
import tnt.tarkovcraft.core.client.screen.navigation.OptionalNavigationEntry;

import java.util.HashSet;
import java.util.Set;

@Mod(value = GroundedInventory.MOD_ID, dist = Dist.CLIENT)
public final class GroundedInventoryClient {

    /** Order 20: after Statistics (10), before Health (25) and Skills (100). */
    public static final NavigationEntry INVENTORY = new OptionalNavigationEntry(
            Component.translatable("screen.grounded_inventory.inventory.title"),
            // Only your own inventory can be shown (the client has no menu for other players).
            (_, userId) -> {
                Minecraft mc = Minecraft.getInstance();
                return mc.player != null && userId.equals(mc.player.getUUID());
            },
            (_, _) -> new InventoryTabScreen(Minecraft.getInstance().player),
            20
    );

    /** Optional direct key for the Inventory tab. Unbound by default. */
    public static final KeyMapping KEY_OPEN_INVENTORY_TAB = new KeyMapping(
            "key.grounded_inventory.open_inventory_tab",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            TarkovCraftCoreClient.SHARED_CATEGORY
    );

    private static volatile Set<Identifier> hiddenLayers = Set.of();

    public GroundedInventoryClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::registerKeyBinds);
        modEventBus.addListener(ModConfigEvent.Loading.class, e -> reloadHiddenLayers());
        modEventBus.addListener(ModConfigEvent.Reloading.class, e -> reloadHiddenLayers());
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, this::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(this::onRenderGuiLayer);
        CoreNavigators.CHARACTER_NAVIGATION_PROVIDER.register(INVENTORY);
    }

    private static void reloadHiddenLayers() {
        Set<Identifier> set = new HashSet<>();
        for (String id : ClientConfig.HIDDEN_HUD_LAYERS.get()) {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null) set.add(parsed);
        }
        hiddenLayers = Set.copyOf(set);
    }

    private void registerKeyBinds(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_INVENTORY_TAB);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY_OPEN_INVENTORY_TAB.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new InventoryTabScreen(mc.player));
            }
        }
    }

    /** E (or anything else opening the plain survival InventoryScreen) opens our tab instead. */
    private void onScreenOpening(ScreenEvent.Opening event) {
        if (!ClientConfig.REPLACE_VANILLA_INVENTORY.get()) return;
        // exact class: leave creative inventory, horse/other subclasses and mod screens alone
        if (event.getNewScreen() == null || event.getNewScreen().getClass() != InventoryScreen.class) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.hasInfiniteMaterials()) return;
        event.setNewScreen(new InventoryTabScreen(player));
    }

    private void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ClientConfig.HIDE_HUD_BARS.get()) return;
        if (hiddenLayers.contains(event.getName())) {
            event.setCanceled(true);
        }
    }
}
