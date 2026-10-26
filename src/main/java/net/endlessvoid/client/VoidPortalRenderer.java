package net.endlessvoid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.endlessvoid.portal.VoidPortalBlock;
import net.endlessvoid.portal.VoidPortalBlockEntity;
import org.joml.Matrix4f;

/**
 * Draws a Nether-portal-thick pane with the End portal shader ({@link RenderType#endPortal()}),
 * the same render type vanilla's TheEndPortalRenderer uses. Only the two broad faces are drawn;
 * the edges always sit against frame blocks or other portal blocks.
 */
public class VoidPortalRenderer implements BlockEntityRenderer<VoidPortalBlockEntity> {
    private static final float NEAR = 6.0F / 16.0F;
    private static final float FAR = 10.0F / 16.0F;

    public VoidPortalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VoidPortalBlockEntity portal, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = portal.getBlockState();
        if (!(state.getBlock() instanceof VoidPortalBlock)) return;

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.endPortal());

        // Vertices wind counter-clockwise as seen from outside each face (the render type culls back faces).
        if (state.getValue(VoidPortalBlock.AXIS) == Direction.Axis.X) {
            quad(consumer, pose, 0, 0, FAR, 1, 0, FAR, 1, 1, FAR, 0, 1, FAR);         // south face
            quad(consumer, pose, 0, 1, NEAR, 1, 1, NEAR, 1, 0, NEAR, 0, 0, NEAR);     // north face
        } else {
            quad(consumer, pose, FAR, 1, 0, FAR, 1, 1, FAR, 0, 1, FAR, 0, 0);         // east face
            quad(consumer, pose, NEAR, 0, 0, NEAR, 0, 1, NEAR, 1, 1, NEAR, 1, 0);     // west face
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        // The end portal render type uses the POSITION vertex format, so position is all it needs.
        consumer.addVertex(pose, x0, y0, z0);
        consumer.addVertex(pose, x1, y1, z1);
        consumer.addVertex(pose, x2, y2, z2);
        consumer.addVertex(pose, x3, y3, z3);
    }
}
