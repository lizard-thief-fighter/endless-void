package net.voidmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.voidmod.VoidMod;
import net.voidmod.registry.VoidBlockEntities;

@EventBusSubscriber(modid = VoidMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VoidClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(VoidBlockEntities.VOID_PORTAL.get(), VoidPortalRenderer::new);
    }

    private VoidClient() {}
}
