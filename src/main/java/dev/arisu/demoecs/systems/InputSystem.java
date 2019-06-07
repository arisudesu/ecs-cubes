package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.InputState;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.components.Velocity;
import dev.arisu.demoecs.util.Pair;
import java.util.concurrent.ArrayBlockingQueue;

public class InputSystem extends EntitySystem {

    public static final class MouseMove extends Pair<Double, Double> {
        public MouseMove(Double x, Double y) {
            super(x, y);
        }
    }

    private ImmutableArray<Entity> playerEntity;

    private ComponentMapper<Rotation> rm = ComponentMapper.getFor(Rotation.class);
    private ComponentMapper<Velocity> vm = ComponentMapper.getFor(Velocity.class);

    private final InputState inputState;
    private final ArrayBlockingQueue<MouseMove> mouseMoves;

    public InputSystem(InputState inputState,
                       ArrayBlockingQueue<MouseMove> mouseMoves) {
        this.inputState = inputState;
        this.mouseMoves = mouseMoves;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntity = engine.getEntitiesFor(Family.all(
                PlayerTag.class,
                Rotation.class
        ).get());
    }

    @Override
    public void update(float deltaTime) {
        final Entity player = playerEntity.first();
        final Rotation rotation = rm.get(player);

        Velocity velocity = vm.get(player);
        if (velocity == null) {
            velocity = new Velocity();
            player.add(velocity);
        }

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

        float deltaX = 0.0f, deltaY = 0.0f;

        if (inputState.isW()) {
            deltaY += Math.sin(Math.toRadians(rotation.yaw));
            deltaX += Math.cos(Math.toRadians(rotation.yaw));
        }
        if (inputState.isS()) {
            deltaY -= Math.sin(Math.toRadians(rotation.yaw));
            deltaX -= Math.cos(Math.toRadians(rotation.yaw));
        }
        if (inputState.isA()) {
            deltaX += Math.sin(Math.toRadians(-rotation.yaw));
            deltaY += Math.cos(Math.toRadians(rotation.yaw));
        }
        if (inputState.isD()) {
            deltaX -= Math.sin(Math.toRadians(-rotation.yaw));
            deltaY -= Math.cos(Math.toRadians(rotation.yaw));
        }

        // normalize input vector
        final float EPS = 0.0000001f;

        if (Math.abs(deltaX) > EPS || Math.abs(deltaY) > EPS) {
            float lenXY = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            deltaX /= lenXY;
            deltaY /= lenXY;
        }

        velocity.x = deltaX * 0.7f;
        velocity.y = deltaY * 0.7f;

        if (Math.abs(velocity.z) < EPS && inputState.isSpace()) {
            inputState.setSpace(false);
            velocity.z = 1.8f;
        }

        velocity.z += -1.25f * deltaTime;
    }
}
