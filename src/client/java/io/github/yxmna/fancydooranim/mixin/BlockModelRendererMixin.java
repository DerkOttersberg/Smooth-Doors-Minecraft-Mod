package io.github.derk.smoothdoors.mixin;

import io.github.derk.smoothdoors.DoorAnimationTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(value=EnvType.CLIENT)
@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {
    private static final Logger FDA_LOG = LoggerFactory.getLogger("smooth-doors");
    private static boolean loggedOnce = false;
    
    // Target the public render() method - 1.21.11 signature
    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void fancydooranim$cancelDoorRender(
        BlockRenderView world,
        List<?> parts,
        BlockState state,
        BlockPos pos,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        boolean cull,
        int overlay,
        CallbackInfo ci
    ) {
        // Log ONCE to confirm mixin is being called
        if (!loggedOnce) {
            FDA_LOG.info("[FDA][MIXIN] BlockModelRenderer mixin IS ACTIVE - rendering block at {}", pos);
            loggedOnce = true;
        }
        
        // Check if it's a door
        if (state.getBlock() instanceof DoorBlock) {
            boolean hidden = DoorAnimationTracker.isDoorHiddenAt(pos);
            FDA_LOG.info("[FDA][RENDER] Door at {} - hidden={}", pos, hidden);
            
            if (hidden) {
                FDA_LOG.info("[FDA][RENDER] *** CANCELING render for door at {} ***", pos);
                ci.cancel();
            }
        }
    }
}
