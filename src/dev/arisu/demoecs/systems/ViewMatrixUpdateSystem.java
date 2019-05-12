package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import org.joml.Matrix4f;

public class ViewMatrixUpdateSystem extends EntitySystem {
    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<Rotation> rm = ComponentMapper.getFor(Rotation.class);

    private ImmutableArray<Entity> playerEntity;

    private final ViewMatrixResource viewMatrixResource;

    public ViewMatrixUpdateSystem(ViewMatrixResource viewMatrixResource) {
        this.viewMatrixResource = viewMatrixResource;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntity = engine.getEntitiesFor(Family.all(PlayerTag.class).get());
    }

    @Override
    public void update(float deltaTime) {
        Entity player = playerEntity.first();
        Position pPos = pm.get(player);
        Rotation pRot = rm.get(player);

        Matrix4f viewMatrix = new Matrix4f();

        // rotate to "normal" view where:
        //  Z is height,
        //  X is width
        //  Y is depth
        viewMatrix.rotate((float) Math.toRadians(90.0f), 0.0f, 1.0f, 0.0f);
        viewMatrix.rotate((float) Math.toRadians(-90.0f), 1.0f, 0.0f, 0.0f);

        viewMatrix.rotateY((float) Math.toRadians(-pRot.pitch));
        viewMatrix.rotateZ((float) Math.toRadians(-pRot.yaw));
        viewMatrix.translate(-pPos.x, -pPos.y, -pPos.z);
        viewMatrix.translate(0.0f, 0.0f, -1.5f);

        /*viewMatrix
                .rotateX((float) Math.toRadians(pRot.pitch))
                .rotateY((float) Math.toRadians(-pRot.yaw))
                .rotateY((float) Math.toRadians(90.0f))
                .rotateX((float) Math.toRadians(-90.0f));

        viewMatrix.translate(-pPos.x, -pPos.y, -pPos.z
                //- 1.0f
        );*/

        viewMatrixResource.setViewMatrix(viewMatrix);
    }
}
