package io.github.derk.smoothdoors.mixin;

import io.github.derk.smoothdoors.DoorAnimMath;
import io.github.derk.smoothdoors.DoorAnimationTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value=ClientWorld.class)
public class ClientWorldDoorTrackMixin {
    private static final Logger FDA_LOG = LoggerFactory.getLogger("smooth-doors");

    private void fancydooranim$trackDoor(BlockPos pos, boolean opening, DoorHinge hinge, DoubleBlockHalf half, Direction facing) {
        DoorAnimationTracker.put(pos, opening, hinge, half, facing);
        if (FDA_LOG.isDebugEnabled()) {
            DoorAnimMath.Pivot pivot = DoorAnimMath.hingePivot(facing, hinge);
            float target = DoorAnimMath.targetAngleDeg(facing, hinge, opening);
            FDA_LOG.debug("[FDA] door math -> target={} °, pivot={}, facing={}, hinge={}", target, pivot, facing, hinge);
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null && mc.worldRenderer != null) {
            BlockState s0 = mc.world.getBlockState(pos);
            mc.worldRenderer.updateBlock(null, pos, s0, s0, 0);
            BlockPos pu = pos.up();
            BlockState s1 = mc.world.getBlockState(pu);
            mc.worldRenderer.updateBlock(null, pu, s1, s1, 0);
        }
    }

    @Inject(method="setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at=@At("HEAD"))
    private void fancydooranim$onSetBlockState(BlockPos pos, BlockState newState, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        ClientWorld world = (ClientWorld)(Object)this;
        BlockState oldState = world.getBlockState(pos);
        
        if (!(newState.getBlock() instanceof DoorBlock)) {
            return;
        }
        
        boolean wasOpen = oldState.getBlock() instanceof DoorBlock && oldState.get(DoorBlock.OPEN);
        boolean isOpen = newState.get(DoorBlock.OPEN);
        
        if (wasOpen == isOpen) {
            return;
        }
        
        if (newState.contains(Properties.DOUBLE_BLOCK_HALF) && 
            newState.contains(Properties.DOOR_HINGE) && 
            newState.contains(Properties.HORIZONTAL_FACING)) {
            
            DoubleBlockHalf half = newState.get(Properties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.LOWER) {
                DoorHinge hinge = newState.get(Properties.DOOR_HINGE);
                Direction facing = newState.get(Properties.HORIZONTAL_FACING);
                if (FDA_LOG.isDebugEnabled()) {
                    FDA_LOG.debug("[FDA] TOGGLE at {} -> isOpen={}", pos, isOpen);
                }
                fancydooranim$trackDoor(pos, isOpen, hinge, half, facing);
            }
        } else {
            fancydooranim$trackDoor(pos, isOpen, DoorHinge.LEFT, DoubleBlockHalf.LOWER, Direction.NORTH);
        }
    }
}
