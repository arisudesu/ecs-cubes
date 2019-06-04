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

        if (deltaX == 0.0f && deltaY == 0.0f && deltaZ == 0.0f) {
            return;
        }

        final List<AABB> crossedCubes = sweepBroadPhase(new AABB(boundingBox, position), deltaX, deltaY, deltaZ);

        // for three planes
        for (int plane = 0; plane < 3; ++plane) {
            final AABB playerAABB = new AABB(boundingBox, position);

            final ArrayList<Pair<Float, Normal>> sweepHits = new ArrayList<>();

            float nearestTime = 1.0f;
            Normal nearestNormal = null;

            for (AABB crossedCubeAABB : crossedCubes) {
                final Pair<Float, Normal> sweep = sweepTestAABB(
                        crossedCubeAABB, playerAABB,
                        position.x, position.y, position.z,
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

            if (nearestNormal != null) {
                applyX += nearestNormal.x * 0.0005;
                applyY += nearestNormal.y * 0.0005;
                applyZ += nearestNormal.z * 0.0005;
            }

            position.x += applyX;
            position.y += applyY;
            position.z += applyZ;

            deltaX -= applyX;
            deltaY -= applyY;
            deltaZ -= applyZ;

            if (nearestTime < 1.0f && nearestTime > 0.0f) {
                System.err.println(String.format("Moved by %.6f with %.6f %.6f", nearestTime, deltaX, deltaY));
                System.err.flush();
            }

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
            } else {
                break;
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
        final boolean noX = Math.abs(dx) < EPS;
        final boolean noY = Math.abs(dy) < EPS;
        final boolean noZ = Math.abs(dz) < EPS;

        if (noX && noY) {
            return null;
        }

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
        final float txmin = (((dx >= 0.0f) ? xmin : xmax) - ox) * invdx;
        final float txmax = (((dx >= 0.0f) ? xmax : xmin) - ox) * invdx;
        final float tymin = (((dy >= 0.0f) ? ymin : ymax) - oy) * invdy;
        final float tymax = (((dy >= 0.0f) ? ymax : ymin) - oy) * invdy;
        final float tzmin = (((dz >= 0.0f) ? zmin : zmax) - oz) * invdz;
        final float tzmax = (((dz >= 0.0f) ? zmax : zmin) - oz) * invdz;

        assert noX || txmin <= txmax;
        assert noY || tymin <= tymax;
        assert noZ || tzmin <= tzmax;

        final Normal normalx = (dx >= 0.0f) ? Normal.WEST : Normal.EAST;
        final Normal normaly = (dy >= 0.0f) ? Normal.SOUTH : Normal.NORTH;
        final Normal normalz = (dz >= 0.0f) ? Normal.DOWN : Normal.UP;

        float tmin, tmax;

        if (noY) {

            // не лежит между плоскостей Y
            if (oy >= ymax || oy <= ymin) return null;

            // txmin > 1.0f => плоскость далеко впереди
            // txmax < 0.0f => плоскость далеко позади
            if (txmin > 1.0f || txmax < 0.0f) return null;

            // для пересечения tmin должно лежать в промежутке [0.0f, 1.0f)
            return new Pair<>(clamp(txmin), normalx);

        } else if (noX) {

            // не лежит между плоскостей X
            if (ox >= xmax || ox <= xmin) return null;

            // tymin > 1.0f => плоскость далеко впереди
            // tymax < 0.0f => плоскость далеко позади
            if (tymin > 1.0f || tymax < 0.0f) return null;

            // для пересечения tmin должно лежать в промежутке [0.0f, 1.0f)
            return new Pair<>(clamp(tymin), normaly);

        } else {

            // условие пересечения
            if (txmin > tymax || txmax < tymin) return null;

            tmin = Math.max(txmin, tymin);
            tmax = Math.min(txmax, tymax);

            if (tmin > 1.0f || tmax < 0.0f) return null;

            return new Pair<>(clamp(tmin), (txmin > tymin) ? normalx : normaly);
        }
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
