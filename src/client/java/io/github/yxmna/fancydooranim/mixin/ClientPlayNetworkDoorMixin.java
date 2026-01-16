package io.github.yxmna.fancydooranim.mixin;

import io.github.yxmna.fancydooranim.DoorAnimMath;
import io.github.yxmna.fancydooranim.DoorAnimationTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value=ClientPlayNetworkHandler.class)
public class ClientPlayNetworkDoorMixin {
    private static final Logger FDA_LOG = LoggerFactory.getLogger("fancy-door-anim");

    @Inject(method="onBlockUpdate", at=@At("HEAD"))
    private void fancydooranim$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc != null ? mc.world : null;
        if (world == null) {
            return;
        }
        handleDoorStateChange(world, packet.getPos(), packet.getState());
    }

    @Inject(method="onChunkDeltaUpdate", at=@At("HEAD"))
    private void fancydooranim$onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc != null ? mc.world : null;
        if (world == null) {
            return;
        }
        packet.visitUpdates((pos, state) -> handleDoorStateChange(world, pos, state));
    }

    private static void handleDoorStateChange(final ClientWorld world, BlockPos pos, final BlockState newState) {
        if (!(newState.getBlock() instanceof DoorBlock)) {
            return;
        }
        
        BlockState oldState = world.getBlockState(pos);
        if (!newState.contains(Properties.OPEN)) {
            return;
        }
        
        boolean wasOpen = oldState.getBlock() instanceof DoorBlock && oldState.get(Properties.OPEN);
        boolean isOpen = newState.get(Properties.OPEN);
        
        if (wasOpen == isOpen) {
            return;
        }
        
        DoubleBlockHalf half = newState.contains(Properties.DOUBLE_BLOCK_HALF) ? 
            newState.get(Properties.DOUBLE_BLOCK_HALF) : DoubleBlockHalf.LOWER;
        DoorHinge hinge = newState.contains(Properties.DOOR_HINGE) ? 
            newState.get(Properties.DOOR_HINGE) : DoorHinge.LEFT;
        Direction facing = newState.contains(Properties.HORIZONTAL_FACING) ? 
            newState.get(Properties.HORIZONTAL_FACING) : Direction.NORTH;
        
        final BlockPos basePos = half == DoubleBlockHalf.UPPER ? pos.down() : pos;
        DoorAnimationTracker.put(basePos, isOpen, hinge, DoubleBlockHalf.LOWER, facing);
        
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                if (mc.worldRenderer == null) {
                    return;
                }
                BlockState lowerNew = newState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                BlockPos upPos = basePos.up();
                BlockState upperNew = newState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                BlockState oldLower = world.getBlockState(basePos);
                mc.worldRenderer.updateBlock(null, basePos, oldLower, lowerNew, 0);
                BlockState oldUpper = world.getBlockState(upPos);
                mc.worldRenderer.updateBlock(null, upPos, oldUpper, upperNew, 0);
            });
        }
        
        if (FDA_LOG.isDebugEnabled()) {
            float target = DoorAnimMath.targetAngleDeg(facing, hinge, isOpen);
            FDA_LOG.debug("[FDA][NET] pos={} base={} wasOpen={} -> isOpen={} target={}deg", pos, basePos, wasOpen, isOpen, target);
        }
    }
}
