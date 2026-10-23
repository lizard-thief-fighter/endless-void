package net.voidmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.voidmod.VoidMod;
import net.voidmod.registry.VoidBlockEntities;

// NeoForge routes mod-bus events (like RegisterRenderers) to the mod bus automatically.
@EventBusSubscriber(modid = VoidMod.MOD_ID, value = Dist.CLIENT)
public final class VoidClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(VoidBlockEntities.VOID_PORTAL.get(), VoidPortalRenderer::new);
    }

    private VoidClient() {}
}
