package io.github.yxmna.fancydooranim;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

@Environment(value=EnvType.CLIENT)
public class DoorAnimationTracker {
    private static final Logger LOG = LoggerFactory.getLogger("fancy-door-anim");
    public static final long ANIM_DURATION_NANOS = 240000000L;
    public static final long REVEAL_LEAD_NANOS = 50000000L;
    private static final long DEDUPE_WINDOW_NANOS = 150000000L;
    private static final Map<BlockPos, Entry> entries = new HashMap<>();

    public static void put(BlockPos pos, boolean opening, DoorHinge hinge, DoubleBlockHalf half, Direction facing) {
        long now = System.nanoTime();
        Entry existing = entries.get(pos);
        if (existing != null && existing.opening == opening && existing.hinge == hinge && existing.facing == facing && now - existing.startNanos < DEDUPE_WINDOW_NANOS) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[FDA] dedupe {} at {} (duplicate trigger suppressed)", opening ? "OPEN" : "CLOSE", pos);
            }
            return;
        }
        entries.put(pos, new Entry(opening, now, hinge, half, facing));
        if (LOG.isDebugEnabled()) {
            LOG.debug("[FDA] put {} at {} hinge={} half={} facing={} size={}", opening ? "OPEN" : "CLOSE", pos, hinge, half, facing, entries.size());
        }
    }

    public static void pruneExpired() {
        long now = System.nanoTime();
        Iterator<Map.Entry<BlockPos, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Entry> e = it.next();
            if (now - e.getValue().startNanos > ANIM_DURATION_NANOS) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        entries.clear();
        if (LOG.isDebugEnabled()) {
            LOG.debug("[FDA] clearAll");
        }
    }

    public static Float computeAngleDeg(BlockPos pos) {
        Entry e = entries.get(pos);
        if (e == null) {
            return null;
        }
        long now = System.nanoTime();
        if (!e.revealScheduled && now >= e.hideUntilNanos) {
            e.revealScheduled = true;
            requestRerender(pos);
        }
        if (now - e.startNanos >= ANIM_DURATION_NANOS) {
            entries.remove(pos);
            requestRerender(pos);
            return null;
        }
        return DoorAnimMath.currentAngleDeg(e.facing, e.hinge, e.opening, e.startNanos, now, ANIM_DURATION_NANOS);
    }

    public static void forEachActive(BiConsumer<BlockPos, Entry> consumer) {
        HashMap<BlockPos, Entry> snapshot = new HashMap<>(entries);
        for (Map.Entry<BlockPos, Entry> it : snapshot.entrySet()) {
            consumer.accept(it.getKey(), it.getValue());
        }
    }

    public static boolean isDoorHiddenAt(BlockPos pos) {
        long now = System.nanoTime();
        Entry e = entries.get(pos);
        if (e != null && now < e.hideUntilNanos) {
            return true;
        }
        Entry lower = entries.get(pos.down());
        return lower != null && now < lower.hideUntilNanos;
    }

    public static boolean isAnimating(BlockPos pos) {
        if (entries.containsKey(pos)) {
            return true;
        }
        return entries.containsKey(pos.down());
    }

    private static void requestRerender(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.worldRenderer == null) {
            return;
        }
        forceRerenderPos(mc, pos);
        forceRerenderPos(mc, pos.up());
    }

    private static void forceRerenderPos(MinecraftClient mc, BlockPos p) {
        try {
            BlockState s = mc.world.getBlockState(p);
            if (s == null) {
                return;
            }
            if (s.contains(Properties.DOUBLE_BLOCK_HALF)) {
                BlockState sFlip = s.cycle(Properties.DOUBLE_BLOCK_HALF);
                mc.worldRenderer.updateBlock(null, p, s, sFlip, 0);
                mc.worldRenderer.updateBlock(null, p, sFlip, s, 0);
                return;
            }
            if (s.contains(Properties.DOOR_HINGE)) {
                BlockState sFlip = s.cycle(Properties.DOOR_HINGE);
                mc.worldRenderer.updateBlock(null, p, s, sFlip, 0);
                mc.worldRenderer.updateBlock(null, p, sFlip, s, 0);
                return;
            }
            mc.worldRenderer.updateBlock(null, p, s, s, 0);
        } catch (Throwable t) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("[FDA] rerender failed at {}: {}", p, t.toString());
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static final class Entry {
        public final boolean opening;
        public final long startNanos;
        public final DoorHinge hinge;
        public final DoubleBlockHalf half;
        public final Direction facing;
        public final long hideUntilNanos;
        public boolean revealScheduled = false;

        public Entry(boolean opening, long startNanos, DoorHinge hinge, DoubleBlockHalf half, Direction facing) {
            this.opening = opening;
            this.startNanos = startNanos;
            this.hinge = hinge;
            this.half = half;
            this.facing = facing;
            long end = startNanos + ANIM_DURATION_NANOS;
            long lead = Math.min(REVEAL_LEAD_NANOS, Math.max(0L, ANIM_DURATION_NANOS - 1000000L));
            this.hideUntilNanos = Math.max(startNanos, end - lead);
        }
    }
}
