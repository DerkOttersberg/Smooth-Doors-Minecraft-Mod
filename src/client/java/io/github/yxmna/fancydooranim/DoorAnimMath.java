package io.github.yxmna.fancydooranim;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.util.math.Direction;

@Environment(value=EnvType.CLIENT)
public final class DoorAnimMath {
    public static final float DOOR_THICKNESS = 0.1875f;

    private DoorAnimMath() {
    }

    public static Inset lateralInsetTowardHinge(Direction facing, DoorHinge hinge, float angleDeg) {
        float a = Math.abs(angleDeg);
        float inset = DOOR_THICKNESS * (float)Math.sin(Math.toRadians(a));
        Pivot p = hingePivot(facing, hinge);
        float dx = 0.0f;
        float dz = 0.0f;
        switch (facing) {
            case NORTH:
            case SOUTH:
                dx = p.x < 0.5f ? inset : -inset;
                break;
            case EAST:
            case WEST:
                dz = p.z < 0.5f ? inset : -inset;
                break;
        }
        return new Inset(dx, dz);
    }

    public static float targetAngleDeg(Direction facing, DoorHinge hinge, boolean opening) {
        int sign = angleSign(facing, hinge);
        return opening ? 90.0f * (float)sign : 0.0f;
    }

    public static int angleSign(Direction facing, DoorHinge hinge) {
        return hinge == DoorHinge.LEFT ? 1 : -1;
    }

    public static Pivot hingePivot(Direction facing, DoorHinge hinge) {
        switch (facing) {
            case NORTH:
                return hinge == DoorHinge.LEFT ? new Pivot(0.0f, 1.0f) : new Pivot(1.0f, 1.0f);
            case EAST:
                return hinge == DoorHinge.LEFT ? new Pivot(0.0f, 0.0f) : new Pivot(0.0f, 1.0f);
            case SOUTH:
                return hinge == DoorHinge.LEFT ? new Pivot(1.0f, 0.0f) : new Pivot(0.0f, 0.0f);
            case WEST:
                return hinge == DoorHinge.LEFT ? new Pivot(1.0f, 1.0f) : new Pivot(1.0f, 0.0f);
        }
        return new Pivot(0.0f, 0.0f);
    }

    public static float ease01(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float u = 1.0f - t;
        return 1.0f - u * u * u;
    }

    public static float currentAngleDeg(Direction facing, DoorHinge hinge, boolean opening, long startNanos, long nowNanos, long durationNanos) {
        float t = (float)(nowNanos - startNanos) / (float)durationNanos;
        float k = ease01(t);
        int sign = angleSign(facing, hinge);
        float target = 90.0f * (float)sign;
        return opening ? k * target : (1.0f - k) * target;
    }

    @Environment(value=EnvType.CLIENT)
    public static final class Pivot {
        public final float x;
        public final float z;

        public Pivot(float x, float z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public String toString() {
            return "Pivot{x=" + x + ", z=" + z + "}";
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static final class Inset {
        public final float dx;
        public final float dz;

        public Inset(float dx, float dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }
}
