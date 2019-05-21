package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.BoundingBox;
import dev.arisu.demoecs.components.Movement;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.terrain.Terrain;
import dev.arisu.demoecs.util.Pair;
import java.util.ArrayList;
import java.util.List;

public class MoveSystem extends EntitySystem {
    private ImmutableArray<Entity> playerEntity;

    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<BoundingBox> bbm = ComponentMapper.getFor(BoundingBox.class);
    private ComponentMapper<Movement> mm = ComponentMapper.getFor(Movement.class);

    private final Terrain terrain;

    public MoveSystem(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntity = engine.getEntitiesFor(Family.all(
                PlayerTag.class,
                Position.class,
                Movement.class,
                BoundingBox.class
        ).get());
    }

    @Override
    public void update(float deltaTime) {
        final Entity player = playerEntity.first();

        final Position position = pm.get(player);
        final BoundingBox boundingBox = bbm.get(player);
        final Movement movement = mm.get(player);

        float deltaX = movement.x,
                deltaY = movement.y,
                deltaZ = movement.z;

        if (deltaX == 0.0f && deltaY == 0.0f && deltaZ == 0.0f) {
            return;
        }

        final AABB originPlayerAABB = new AABB(boundingBox, position);

        final List<AABB> crossedCubes = sweepBroadPhase(originPlayerAABB, deltaX, deltaY, deltaZ);

        // for three planes
        for (int plane = 0; plane < 3; ++plane) {
            final AABB playerAABB = new AABB(boundingBox, position);

            final ArrayList<Pair<Float, Normal>> sweepHits = new ArrayList<>();

            for (AABB crossedCubeAABB : crossedCubes) {
                final Pair<Float, Normal> sweep = sweepTestAABB(
                        crossedCubeAABB, playerAABB,
                        position.x, position.y, position.z,
                        deltaX, deltaY, deltaZ, 0.000f, 0.000f, 0.000f);

                if (sweep == null) {
                    continue;
                }
                sweepHits.add(sweep);
            }

            float nearestTime = 1.0f;
            Normal nearestNormal = null;

            if (!sweepHits.isEmpty()) {
                System.out.println(deltaX + ", " + deltaY + ": " + sweepHits);
            }

            for (Pair<Float, Normal> sweepHit : sweepHits) {
                assert sweepHit.getA() == 1.0f || sweepHit.getB() != null;

                if (sweepHit.getA() < nearestTime) {
                    nearestTime = sweepHit.getA();
                    nearestNormal = sweepHit.getB();
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
     * @param fixed   {@link AABB} that isn't moving.
     * @param dynamic {@link AABB} that is moving.
     * @param ox      Origin of velocity vector.
     * @param oy      Origin of velocity vector.
     * @param oz      Origin of velocity vector.
     * @param dx      Velocity vector.
     * @param dy      Velocity vector.
     * @param dz      Velocity vector.
     * @param marginX Additional margin to fixed AABB that doesn't allow objects to become too close.
     * @param marginY Additional margin to fixed AABB that doesn't allow objects to become too close.
     * @param marginZ Addition
     *                al margin to fixed AABB that doesn't allow objects to become too close.
     * @return
     */
    private static Pair<Float, Normal> sweepTestAABB(AABB fixed, AABB dynamic,
                                                     float ox, float oy, float oz,
                                                     float dx, float dy, float dz,
                                                     float marginX, float marginY, float marginZ) {
        final float EPS = 0.0000001f;
//        final boolean noZ = Math.abs(dz) < EPS;
        final boolean noY = Math.abs(dy) < EPS;
        final boolean noX = Math.abs(dx) < EPS;

        float invdx = 1 / dx;
        float invdy = 1 / dy;

        if (noX && noY) {
            return null;
        }

        if (noX) {
            float tymin = (((dy < 0.0f) ? (fixed.maxY + dynamic.getHalfDepth() + marginY) : (fixed.minY - dynamic.getHalfDepth() - marginY)) - oy) * invdy;
            float tymax = (((dy < 0.0f) ? (fixed.minY - dynamic.getHalfDepth() - marginY) : (fixed.maxY + dynamic.getHalfDepth() + marginY)) - oy) * invdy;
            float xmax = fixed.maxX + dynamic.getHalfWidth() + marginX;
            float xmin = fixed.minX - dynamic.getHalfWidth() - marginX;

            final Normal normal = (dy >= 0.0f) ? Normal.SOUTH : Normal.NORTH;

            if (ox >= xmax || ox <= xmin) {
                return new Pair<>(1.0f, null);
            }
            if (tymin > 1.0f || tymax < 0.0f) {
                return new Pair<>(1.0f, null);
            }
            tymin = clamp(tymin);
            return new Pair<>(tymin, normal);
        } else if (noY) {

            float tmin = (((dx < 0.0f) ? (fixed.maxX + dynamic.getHalfWidth() + marginX) : (fixed.minX - dynamic.getHalfWidth() - marginX)) - ox) * invdx;
            float tmax = (((dx < 0.0f) ? (fixed.minX - dynamic.getHalfWidth() - marginX) : (fixed.maxX + dynamic.getHalfWidth() + marginX)) - ox) * invdx;
            float ymax = fixed.maxY + dynamic.getHalfDepth() + marginY;
            float ymin = fixed.minY - dynamic.getHalfDepth() - marginY;

            final Normal normal = (dx >= 0.0f) ? Normal.WEST : Normal.EAST;

            if (oy >= ymax || oy <= ymin) {
                return new Pair<>(1.0f, null);
            }
            if (tmin > 1.0f || tmax < 0.0f) {
                return new Pair<>(1.0f, null);
            }
            tmin = clamp(tmin);
            return new Pair<>(tmin, normal);
        } else {
            float txmin = ((dx >= 0.0f ? (fixed.minX - dynamic.getHalfWidth() - marginX) : (fixed.maxX + dynamic.getHalfWidth() + marginX)) - ox) * invdx;
            float txmax = ((dx >= 0.0f ? (fixed.maxX + dynamic.getHalfWidth() + marginX) : (fixed.minX - dynamic.getHalfWidth() - marginX)) - ox) * invdx;

            float tymin = ((dy >= 0.0f ? (fixed.minY - dynamic.getHalfDepth() - marginY) : (fixed.maxY + dynamic.getHalfDepth() + marginY)) - oy) * invdy;
            float tymax = ((dy >= 0.0f ? (fixed.maxY + dynamic.getHalfDepth() + marginY) : (fixed.minY - dynamic.getHalfDepth() - marginY)) - oy) * invdy;

            if (txmin > tymax || tymin > txmax) {
                return new Pair<>(1.0f, null);
            }

            float tmin = Math.max(txmin, tymin);
            float tmax = Math.min(txmax, tymax);

            if (tmin >= 1.0f || tmax <= 0.0f) {
                return new Pair<>(1.0f, null);
            }

            if (tmin > 1.0f) {
                return new Pair<>(1.0f, null);
            } else {
                tmin = clamp(tmin);

                Normal normal = (txmin > tymin)
                        ? ((dx > 0.0f) ? Normal.WEST : Normal.EAST)
                        : ((dy > 0.0f) ? Normal.SOUTH : Normal.NORTH);

                return new Pair<>(tmin, normal);
            }
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
