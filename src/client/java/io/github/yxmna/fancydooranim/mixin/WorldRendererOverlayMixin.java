package io.github.yxmna.fancydooranim.mixin;

import io.github.yxmna.fancydooranim.render.DoorAnimatedOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value=WorldRenderer.class)
public abstract class WorldRendererOverlayMixin {
    @Shadow
    private BufferBuilderStorage bufferBuilders;
    
    @Shadow
    private ClientWorld world;

    @Inject(
        method="renderBlockEntities",
        at=@At("TAIL"),
        require=0
    )
    private void fancydooranim$afterBlockEntities(
        MatrixStack _ignoredMatrices, 
        WorldRenderState worldRenderState, 
        OrderedRenderCommandQueueImpl _ignoredQueue, 
        CallbackInfo ci
    ) {
        if (this.world == null) {
            return;
        }
        
        MatrixStack matrices = new MatrixStack();
        VertexConsumerProvider.Immediate consumers = this.bufferBuilders.getEntityVertexConsumers();
        
        // Get camera position from game renderer - this is the actual view position
        Vec3d camPos = Vec3d.ZERO;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            Camera camera = mc.gameRenderer.getCamera();
            camPos = ((CameraAccessor) camera).getPos();
        }
        
        DoorAnimatedOverlay.renderAll(this.world, matrices, camPos, consumers);
    }
}
