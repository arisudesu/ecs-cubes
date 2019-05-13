package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.terrain.Terrain;

public class GravitySystem extends EntitySystem {
    private final Terrain terrain;
    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);

    private ImmutableArray<Entity> entities;

    public GravitySystem(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(
                PlayerTag.class
        ).get());
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : entities) {
            Position position = pm.get(entity);
            position.z -= 0.98f * deltaTime;

            int
                    checkX = (int) position.x,
                    checkY = (int) position.y,
                    checkZ = (int) position.z;

            if (position.x < 0) {
                checkX--;
            }
            if (position.y < 0) {
                checkY--;
            }

            byte block = terrain.getBlock(checkX, checkY, checkZ);

            if (block != 0) {
                position.z = checkZ + 1;
            }

            if (position.z < 0.0f) {
                position.z = 0.0f;
            }
        }
    }
}
