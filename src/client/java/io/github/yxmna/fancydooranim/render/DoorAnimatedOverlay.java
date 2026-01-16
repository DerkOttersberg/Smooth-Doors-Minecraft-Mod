package io.github.yxmna.fancydooranim.render;

import io.github.yxmna.fancydooranim.DoorAnimMath;
import io.github.yxmna.fancydooranim.DoorAnimationTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.world.BlockRenderView;

@Environment(value=EnvType.CLIENT)
public final class DoorAnimatedOverlay {
    private static final double EPS = 0.0005;

    private DoorAnimatedOverlay() {
    }

    public static void renderAll(ClientWorld world, MatrixStack ms, Vec3d cameraPos, VertexConsumerProvider consumers) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || world == null || consumers == null) {
            return;
        }
        
        BlockRenderManager brm = mc.getBlockRenderManager();
        
        DoorAnimationTracker.forEachActive((pos, entry) -> {
            Float angleDeg = DoorAnimationTracker.computeAngleDeg(pos);
            if (angleDeg == null) {
                return;
            }
            
            BlockState worldState = world.getBlockState(pos);
            if (!(worldState.getBlock() instanceof DoorBlock)) {
                return;
            }
            
            // Create closed door state with the correct hinge and facing
            BlockState lowerClosed = worldState
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                .with(Properties.OPEN, false)
                .with(Properties.DOOR_HINGE, entry.hinge)
                .with(Properties.HORIZONTAL_FACING, entry.facing);
            
            BlockState upperClosed = lowerClosed
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
            
            renderHalfRotated(world, brm, consumers, ms, cameraPos, pos, lowerClosed, angleDeg, entry);
            renderHalfRotated(world, brm, consumers, ms, cameraPos, pos.up(), upperClosed, angleDeg, entry);
        });
    }

    private static void renderHalfRotated(
        BlockRenderView world,
        BlockRenderManager brm,
        VertexConsumerProvider consumers,
        MatrixStack ms,
        Vec3d cam,
        BlockPos pos,
        BlockState stateClosed,
        float angleDeg,
        DoorAnimationTracker.Entry entry
    ) {
        ms.push();
        
        // Translate to block position relative to camera
        double x = pos.getX() - cam.x;
        double y = pos.getY() - cam.y;
        double z = pos.getZ() - cam.z;
        
        // Small offset toward camera to prevent z-fighting
        double dxCam = cam.x - (pos.getX() + 0.5);
        double dzCam = cam.z - (pos.getZ() + 0.5);
        double len = Math.sqrt(dxCam * dxCam + dzCam * dzCam);
        double ex = len > 1.0E-6 ? dxCam / len * EPS : 0.0;
        double ez = len > 1.0E-6 ? dzCam / len * EPS : 0.0;
        
        ms.translate(x + ex, y, z + ez);
        
        // Apply lateral inset based on rotation
        DoorAnimMath.Inset inset = DoorAnimMath.lateralInsetTowardHinge(entry.facing, entry.hinge, angleDeg);
        ms.translate(inset.dx, 0.0, inset.dz);
        
        // Rotate around hinge pivot
        DoorAnimMath.Pivot p = DoorAnimMath.hingePivot(entry.facing, entry.hinge);
        ms.translate(p.x, 0.0, p.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angleDeg));
        ms.translate(-p.x, 0.0, -p.z);
        
        // Render the door block using BlockRenderManager
        try {
            int light = WorldRenderer.getLightmapCoordinates(world, pos);
            int overlay = OverlayTexture.DEFAULT_UV;
            
            // Render using the block render manager
            brm.renderBlockAsEntity(stateClosed, ms, consumers, light, overlay);
        } catch (Exception e) {
            // Silently ignore rendering errors
        }
        
        ms.pop();
    }
}
