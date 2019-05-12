package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.InputState;
import dev.arisu.demoecs.components.BoundingBox;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.util.Pair;

import java.util.concurrent.ArrayBlockingQueue;

public class MoveSystem extends EntitySystem {

    public static final class MouseMove extends Pair<Double, Double> {
        public MouseMove(Double x, Double y) {
            super(x, y);
        }
    }

    private ImmutableArray<Entity> playerEntity;
    private ImmutableArray<Entity> entities;

    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<Rotation> rm = ComponentMapper.getFor(Rotation.class);
    private ComponentMapper<BoundingBox> bbm = ComponentMapper.getFor(BoundingBox.class);

    private final InputState inputState;
    private final ArrayBlockingQueue<MouseMove> mouseMoves;

    public MoveSystem(InputState inputState,
                      ArrayBlockingQueue<MouseMove> mouseMoves) {
        this.inputState = inputState;
        this.mouseMoves = mouseMoves;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntity = engine.getEntitiesFor(Family.all(
                PlayerTag.class,
                Position.class,
                BoundingBox.class
        ).get());
        entities = engine.getEntitiesFor(Family.all(
                Position.class,
                BoundingBox.class
        ).get());
    }


    @Override
    public void update(float deltaTime) {

        for (Entity player : playerEntity) {
            Position position = pm.get(player);
            Rotation rotation = rm.get(player);
            BoundingBox boundingBox = bbm.get(player);

            while (!mouseMoves.isEmpty()) {
                MouseMove mm = mouseMoves.poll();

                rotation.pitch += mm.getB() * 0.3f;
                rotation.yaw -= mm.getA() * 0.3f;

                if (rotation.pitch > 90.0f) {
                    rotation.pitch = 90.0f;
                } else if (rotation.pitch < -90.0f) {
                    rotation.pitch = -90.0f;
                }
            }

            if (inputState.isW()) {
                position.y += Math.sin(Math.toRadians(rotation.yaw)) * 0.1f;
                position.x += Math.cos(Math.toRadians(rotation.yaw)) * 0.1f;
            }
            if (inputState.isS()) {
                position.y -= Math.sin(Math.toRadians(rotation.yaw)) * 0.1f;
                position.x -= Math.cos(Math.toRadians(rotation.yaw)) * 0.1f;
            }
            if (inputState.isA()) {
                position.x += Math.sin(Math.toRadians(-rotation.yaw)) * 0.1f;
                position.y += Math.cos(Math.toRadians(rotation.yaw)) * 0.1f;
            }
            if (inputState.isD()) {
                position.x += Math.sin(Math.toRadians(-rotation.yaw)) * -0.1f;
                position.y += Math.cos(Math.toRadians(rotation.yaw)) * -0.1f;
            }
            if (inputState.isSpace()) {
                position.z += 0.2;
            }

            for (Entity entity1 : entities) {
                Position position1 = pm.get(entity1);
                BoundingBox boundingBox1 = bbm.get(entity1);

                if (player != entity1 && intersectsAABB(boundingBox1, position1, boundingBox, position)) {
                    System.out.println(player + " intersects " + entity1);
                    getEngine().removeEntity(entity1);
                }
            }
        }
    }

    public static boolean intersectsAABB(BoundingBox ab,
                                         Position ap,
                                         BoundingBox bb,
                                         Position bp) {

        float
                cx0 = ap.x - ab.width / 2,
                cy0 = ap.y - ab.depth / 2,
                cz0 = ap.z,

                cx1 = ap.x + ab.width / 2,
                cy1 = ap.y + ab.depth / 2,
                cz1 = ap.z + ab.height,

                tx0 = bp.x - bb.width / 2,
                ty0 = bp.y - bb.depth / 2,
                tz0 = bp.z,

                tx1 = bp.x + bb.width / 2,
                ty1 = bp.y + bb.depth / 2,
                tz1 = bp.z + bb.height;

        if (cx1 > tx0 && cx0 < tx1) {
            if (cy1 > ty0 && cy0 < ty1) {
                return cz1 > tz0 && cz0 < tz1;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
