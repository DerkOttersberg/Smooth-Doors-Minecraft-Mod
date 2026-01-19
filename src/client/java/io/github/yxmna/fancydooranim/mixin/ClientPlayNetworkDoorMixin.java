package io.github.derk.smoothdoors.mixin;

import io.github.derk.smoothdoors.DoorAnimMath;
import io.github.derk.smoothdoors.DoorAnimationTracker;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(value=EnvType.CLIENT)
@Mixin(value=ClientPlayNetworkHandler.class)
public class ClientPlayNetworkDoorMixin {
    private static final Logger FDA_LOG = LoggerFactory.getLogger("smooth-doors");
    private static final Map<BlockPos, Boolean> doorStateCache = new ConcurrentHashMap<>();

    @Inject(method="onBlockUpdate", at=@At("HEAD"))
    private void fancydooranim$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc != null ? mc.world : null;
        if (world == null) {
            return;
        }
        // Capture old state BEFORE packet is processed
        BlockPos pos = packet.getPos();
        BlockState oldState = world.getBlockState(pos);
        handleDoorStateChange(world, pos, packet.getState(), oldState);
    }

    @Inject(method="onChunkDeltaUpdate", at=@At("HEAD"))
    private void fancydooranim$onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc != null ? mc.world : null;
        if (world == null) {
            return;
        }
        packet.visitUpdates((pos, state) -> {
            BlockState oldState = world.getBlockState(pos);
            handleDoorStateChange(world, pos, state, oldState);
        });
    }

    private static void handleDoorStateChange(final ClientWorld world, BlockPos pos, final BlockState newState, final BlockState oldState) {
        if (!(newState.getBlock() instanceof DoorBlock)) {
            return;
        }
        
        FDA_LOG.info("[FDA][NET] Detected door block update at {}", pos);
        
        if (!newState.contains(Properties.OPEN)) {
            return;
        }
        
        // Get cached old state, or false if not cached
        Boolean cachedWasOpen = doorStateCache.get(pos);
        boolean wasOpen = cachedWasOpen != null ? cachedWasOpen : false;
        boolean isOpen = newState.get(Properties.OPEN);
        
        // Update cache with new state
        doorStateCache.put(pos.toImmutable(), isOpen);
        
        FDA_LOG.info("[FDA][NET] Door at {} - wasOpen={}, isOpen={}", pos, wasOpen, isOpen);
        
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
                // Force chunk rebuilds to hide/show the door during animation
                BlockPos upPos = basePos.up();
                FDA_LOG.info("[FDA][NET] Scheduling chunk rebuilds for door at {} and {}", basePos, upPos);
                
                // Schedule rebuild for the chunk sections containing both door halves
                int chunkX = basePos.getX() >> 4;
                int chunkZ = basePos.getZ() >> 4;
                int sectionY1 = basePos.getY() >> 4;
                int sectionY2 = upPos.getY() >> 4;
                
                mc.worldRenderer.scheduleBlockRenders(chunkX, sectionY1, chunkZ, chunkX, sectionY1, chunkZ);
                if (sectionY2 != sectionY1) {
                    mc.worldRenderer.scheduleBlockRenders(chunkX, sectionY2, chunkZ, chunkX, sectionY2, chunkZ);
                }
            });
        }
        
        if (FDA_LOG.isDebugEnabled()) {
            float target = DoorAnimMath.targetAngleDeg(facing, hinge, isOpen);
            FDA_LOG.debug("[FDA][NET] pos={} base={} wasOpen={} -> isOpen={} target={}deg", pos, basePos, wasOpen, isOpen, target);
        }
    }
}
