package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.components.Scale;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class EntityRenderSystem extends EntitySystem {
    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<Rotation> rm = ComponentMapper.getFor(Rotation.class);
    private ComponentMapper<Scale> sm = ComponentMapper.getFor(Scale.class);

    private ImmutableArray<Entity> entities;

    private ViewMatrixResource viewMatrixResource;

    public EntityRenderSystem(ViewMatrixResource viewMatrixResource) {
        this.viewMatrixResource = viewMatrixResource;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family
                .all(Rotation.class)
                .exclude(PlayerTag.class).get());

    }

    @Override
    public void update(float deltaTime) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        Matrix4f projMatrix = new Matrix4f();

        projMatrix.perspective(
                (float) Math.toRadians(70.0f),
                800.f / 600.f, 0.05f, 1000.0f
        );

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(projMatrix.get(fb));

        glMatrixMode(GL_MODELVIEW);

        for (Entity entity : entities) {
            Position position = pm.get(entity);
            Rotation rotation = rm.get(entity);
            Scale scale = sm.get(entity);

            Matrix4f modelMatrix = new Matrix4f()
                    .translate(position.x, position.y, position.z)
                    .scale(scale.x, scale.y, scale.z);
            Matrix4f modelViewMatrix = new Matrix4f();

            glLoadMatrixf(viewMatrixResource.getViewMatrix().mul(modelMatrix, modelViewMatrix).get(fb));

            glBegin(GL_QUADS);
            glColor3f(0.0f, 0.0f, 1.0f);
            glVertex3f(0.5f, -0.5f, 0.0f);
            glVertex3f(-0.5f, -0.5f, 0.0f);
            glVertex3f(-0.5f, 0.5f, 0.0f);
            glVertex3f(0.5f, 0.5f, 0.0f);

            glColor3f(1.0f, 0.0f, 1.0f);
            glVertex3f(0.5f, -0.5f, 1.0f);
            glVertex3f(0.5f, 0.5f, 1.0f);
            glVertex3f(-0.5f, 0.5f, 1.0f);
            glVertex3f(-0.5f, -0.5f, 1.0f);

            glColor3f(1.0f, 0.0f, 0.0f);
            glVertex3f(0.5f, -0.5f, 0.0f);
            glVertex3f(0.5f, 0.5f, 0.0f);
            glVertex3f(0.5f, 0.5f, 1.0f);
            glVertex3f(0.5f, -0.5f, 1.0f);

            glColor3f(1.0f, 1.0f, 0.0f);
            glVertex3f(-0.5f, -0.5f, 1.0f);
            glVertex3f(-0.5f, 0.5f, 1.0f);
            glVertex3f(-0.5f, 0.5f, 0.0f);
            glVertex3f(-0.5f, -0.5f, 0.0f);

            glColor3f(0.0f, 1.0f, 0.0f);
            glVertex3f(0.5f, 0.5f, 1.0f);
            glVertex3f(0.5f, 0.5f, 0.0f);
            glVertex3f(-0.5f, 0.5f, 0.0f);
            glVertex3f(-0.5f, 0.5f, 1.0f);

            glColor3f(0.0f, 1.0f, 1.0f);
            glVertex3f(0.5f, -0.5f, 0.0f);
            glVertex3f(0.5f, -0.5f, 1.0f);
            glVertex3f(-0.5f, -0.5f, 1.0f);
            glVertex3f(-0.5f, -0.5f, 0.0f);
            glEnd();
        }

        glFlush();
    }
}
