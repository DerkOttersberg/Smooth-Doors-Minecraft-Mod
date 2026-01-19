package io.github.derk.smoothdoors.model;

import io.github.derk.smoothdoors.DoorAnimationTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class DoorHidingModel extends WrapperBlockStateModel implements FabricBlockStateModel {
    public DoorHidingModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(
        QuadEmitter emitter,
        BlockRenderView blockView,
        BlockPos pos,
        BlockState state,
        Random random,
        Predicate<@Nullable Direction> cullTest
    ) {
        if (!(state.getBlock() instanceof DoorBlock)) {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        if (DoorAnimationTracker.isDoorHiddenAt(pos)) {
            return;
        }

        super.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }
}
