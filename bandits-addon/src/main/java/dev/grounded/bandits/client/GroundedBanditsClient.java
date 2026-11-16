package dev.grounded.bandits.client;

import dev.grounded.bandits.GroundedBandits;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = GroundedBandits.MOD_ID, dist = Dist.CLIENT)
public final class GroundedBanditsClient {

    public GroundedBanditsClient(IEventBus modEventBus) {
        modEventBus.addListener(GroundedBanditsClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GroundedBandits.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(GroundedBandits.BANDIT_MARKSMAN.get(), BanditRenderer::new);
        event.registerEntityRenderer(GroundedBandits.SURVIVOR.get(), BanditRenderer::new);
    }
}
