package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.BoundingBox;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Velocity;
import dev.arisu.demoecs.terrain.Terrain;
import dev.arisu.demoecs.util.Pair;
import java.util.ArrayList;
import java.util.List;

public class MoveSystem extends EntitySystem {
    private ImmutableArray<Entity> playerEntity;

    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<BoundingBox> bbm = ComponentMapper.getFor(BoundingBox.class);
    private ComponentMapper<Velocity> vm = ComponentMapper.getFor(Velocity.class);

    private final Terrain terrain;

    public MoveSystem(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntity = engine.getEntitiesFor(Family.all(
                PlayerTag.class,
                Position.class,
                Velocity.class,
                BoundingBox.class
        ).get());
    }

    @Override
    public void update(float deltaTime) {
        final Entity player = playerEntity.first();

        final Position position = pm.get(player);
        final BoundingBox boundingBox = bbm.get(player);
        final Velocity velocity = vm.get(player);

        float deltaX = velocity.x,
                deltaY = velocity.y,
                deltaZ = velocity.z;

//        if (deltaX == 0.0f && deltaY == 0.0f && deltaZ == 0.0f) {
//            return;
//        }

        final List<AABB> crossedCubes = sweepBroadPhase(new AABB(boundingBox, position), deltaX, deltaY, deltaZ);

        // for three planes
        for (int plane = 0; plane < 3; ++plane) {
            final AABB playerAABB = new AABB(boundingBox, position);

            float nearestTime = 1.0f;
            Normal nearestNormal = null;

            for (AABB crossedCubeAABB : crossedCubes) {
                final Pair<Float, Normal> sweep = sweepTestAABB(
                        crossedCubeAABB, playerAABB,
                        position.x, position.y, position.z + playerAABB.getHalfHeight(),
                        deltaX, deltaY, deltaZ);

                if (sweep == null) {
                    continue;
                }

                if (sweep.getA() < nearestTime) {
                    assert sweep.getA() == 1.0f || sweep.getB() != null;

                    nearestTime = sweep.getA();
                    nearestNormal = sweep.getB();
                }
            }

            float applyX = deltaX * nearestTime;
            float applyY = deltaY * nearestTime;
            float applyZ = deltaZ * nearestTime;

//            if (nearestNormal != null) {
//                applyX += nearestNormal.x * 0.0005;
//                applyY += nearestNormal.y * 0.0005;
//                applyZ += nearestNormal.z * 0.0005;
//            }

            position.x += applyX;
            position.y += applyY;
            position.z += applyZ;

            deltaX -= applyX;
            deltaY -= applyY;
            deltaZ -= applyZ;

//            if (nearestTime < 1.0f && nearestTime > 0.0f) {
//                System.err.println(String.format("Moved by %.6f with %.6f %.6f", nearestTime, deltaX, deltaY));
//                System.err.flush();
//            }

            if (nearestNormal == Normal.WEST && deltaX > 0.0f) {
                deltaX = 0.0f;
            } else if (nearestNormal == Normal.EAST && deltaX < 0.0f) {
                deltaX = 0.0f;
            } else if (nearestNormal == Normal.SOUTH && deltaY > 0.0f) {
                deltaY = 0.0f;
            } else if (nearestNormal == Normal.NORTH && deltaY < 0.0f) {
                deltaY = 0.0f;
            } else if (nearestNormal == Normal.DOWN && deltaZ > 0.0f) {
                deltaZ = 0.0f;
            } else if (nearestNormal == Normal.UP && deltaZ < 0.0f) {
                deltaZ = 0.0f;
            }
        }
    }

    /**
     * Find collision of dynamic AABB against static AABB by solving series of velocity equation for t:
     * <pre>t = (x - x<sub>0</sub>) / dx</pre>
     *
     * @param fixed   {@link AABB} that isn't moving.
     * @param dynamic {@link AABB} that is moving.
     * @param ox      Origin of velocity vector.
     * @param oy      Origin of velocity vector.
     * @param oz      Origin of velocity vector.
     * @param dx      Velocity vector.
     * @param dy      Velocity vector.
     * @param dz      Velocity vector.
     * @return {@link Pair}
     */
    private static Pair<Float, Normal> sweepTestAABB(AABB fixed, AABB dynamic,
                                                     float ox, float oy, float oz,
                                                     float dx, float dy, float dz) {
        final float EPS = 0.0000001f;

        final boolean hasX = Math.abs(dx) >= EPS;
        final boolean hasY = Math.abs(dy) >= EPS;
        final boolean hasZ = Math.abs(dz) >= EPS;

        // плоскости наибольшего приближения по каждой из осей в обоих направлениях
        // всегда валидные значения float
        final float xmin = fixed.minX - dynamic.getHalfWidth();
        final float xmax = fixed.maxX + dynamic.getHalfWidth();
        final float ymin = fixed.minY - dynamic.getHalfDepth();
        final float ymax = fixed.maxY + dynamic.getHalfDepth();
        final float zmin = fixed.minZ - dynamic.getHalfHeight();
        final float zmax = fixed.maxZ + dynamic.getHalfHeight();

        // решение уравнения t = (x - x0) / dx для каждой из пересекаемых прямой плоскостей
        // могут принимать значения +Inf, -Inf, NaN в зависимости от параметров
        final float invdx = 1 / dx;
        final float invdy = 1 / dy;
        final float invdz = 1 / dz;
        final float txmin = ((dx >= 0.0f ? xmin : xmax) - ox) * invdx;
        final float txmax = ((dx >= 0.0f ? xmax : xmin) - ox) * invdx;
        final float tymin = ((dy >= 0.0f ? ymin : ymax) - oy) * invdy;
        final float tymax = ((dy >= 0.0f ? ymax : ymin) - oy) * invdy;
        final float tzmin = ((dz >= 0.0f ? zmin : zmax) - oz) * invdz;
        final float tzmax = ((dz >= 0.0f ? zmax : zmin) - oz) * invdz;

        assert !hasX || txmin <= txmax;
        assert !hasY || tymin <= tymax;
        assert !hasZ || tzmin <= tzmax;

        final Normal normalx = (dx >= 0.0f) ? Normal.WEST : Normal.EAST;
        final Normal normaly = (dy >= 0.0f) ? Normal.SOUTH : Normal.NORTH;
        final Normal normalz = (dz >= 0.0f) ? Normal.DOWN : Normal.UP;

        if (hasX && hasY && hasZ) {
            if (txmin > tymax || txmax < tymin)
                return null;

            float tmin = Math.max(txmin, tymin);
            float tmax = Math.min(txmax, tymax);
            Normal normal = (txmin > tymin) ? normalx : normaly;

            if (tmin > tzmax || tmax < tzmin)
                return null;

            normal = (tmin > tzmin) ? normal : normalz;
            tmin = Math.max(tmin, tzmin);
            tmax = Math.min(tmax, tzmax);

            if (tmin >= 1.0f || tmax <= 0.0f)
                return null;

            return new Pair<>(clamp(tmin), normal);

        } else if (hasX && hasY) {
            if (oz >= zmax || oz <= zmin) return null;
            if (txmin > tymax || txmax < tymin) return null;

            float tmin = Math.max(txmin, tymin);
            float tmax = Math.min(txmax, tymax);
            Normal normal = (txmin > tymin) ? normalx : normaly;

            if (tmin >= 1.0f || tmax <= 0.0f) return null;
            return new Pair<>(clamp(tmin), normal);

        } else if (hasX && hasZ) {
            if (oy >= ymax || oy <= ymin) return null;
            if (txmin > tzmax || txmax < tzmin) return null;

            float tmin = Math.max(txmin, tzmin);
            float tmax = Math.min(txmax, tzmax);
            Normal normal = (txmin > tzmin) ? normalx : normalz;

            if (tmin >= 1.0f || tmax <= 0.0f) return null;
            return new Pair<>(clamp(tmin), normal);

        } else if (hasY && hasZ) {
            if (ox >= xmax || ox <= xmin) return null;
            if (tymin > tzmax || tymax < tzmin) return null;

            float tmin = Math.max(tymin, tzmin);
            float tmax = Math.min(tymax, tzmax);
            Normal normal = (tymin > tzmin) ? normaly : normalz;

            if (tmin >= 1.0f || tmax <= 0.0f) return null;
            return new Pair<>(clamp(tmin), normal);

        } else if (hasX) {
            if (oy >= ymax || oy <= ymin) return null;
            if (oz >= zmax || oz <= zmin) return null;
            if (txmin >= 1.0f | txmax <= 0.0f) return null;
            return new Pair<>(clamp(txmin), normalx);

        } else if (hasY) {
            if (ox >= xmax || ox <= xmin) return null;
            if (oz >= zmax || oz <= zmin) return null;
            if (tymin >= 1.0f | tymax <= 0.0f) return null;
            return new Pair<>(clamp(tymin), normaly);

        } else if (hasZ) {
            if (ox >= xmax || ox <= xmin) return null;
            if (oy >= ymax || oy <= ymin) return null;
            if (tzmin >= 1.0f | tzmax <= 0.0f) return null;
            return new Pair<>(clamp(tzmin), normalz);
        }

        return null;
    }

    private List<AABB> sweepBroadPhase(AABB movingAABB, float dx, float dy, float dz) {
        final AABB aabb = movingAABB.expand(dx, dy, dz);

        // TODO: convert it to AABB too
        // TODO: replace with something cheaper than Math.floor()
        int
                minX = (int) Math.floor(aabb.minX),
                minY = (int) Math.floor(aabb.minY),
                minZ = (int) Math.floor(aabb.minZ),
                maxX = (int) aabb.maxX,
                maxY = (int) aabb.maxY,
                maxZ = (int) aabb.maxZ;

        final ArrayList<AABB> boundingBoxes = new ArrayList<>();

        for (int z = minZ; z <= maxZ; ++z) {
            for (int y = minY; y <= maxY; ++y) {
                for (int x = minX; x <= maxX; ++x) {
                    if (terrain.hasBlock(x, y, z)) {
                        boundingBoxes.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return boundingBoxes;
    }

    private static float clamp(float val) {
        return Math.max(0.0f, Math.min(1.0f, val));
    }
}
