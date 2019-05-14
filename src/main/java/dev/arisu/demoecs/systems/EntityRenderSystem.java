package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.components.Scale;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.util.File;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class EntityRenderSystem extends EntitySystem {
    private ComponentMapper<Position> pm = ComponentMapper.getFor(Position.class);
    private ComponentMapper<Rotation> rm = ComponentMapper.getFor(Rotation.class);
    private ComponentMapper<Scale> sm = ComponentMapper.getFor(Scale.class);

    private ImmutableArray<Entity> entities;

    private ViewMatrixResource viewMatrixResource;

    private int program;
    private int vertexLoc;
    private int colorLoc;
    private int projULoc, viewULoc, modelULoc;
    private int buffer;

    private static final float[] CUBE_VERTICES = new float[]{
            0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,

            0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, 0.0f,

            0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 1.0f, 0.0f, 1.0f, 0.0f,
            0.5f, -0.5f, 1.0f, 0.0f, 1.0f, 0.0f,

            -0.5f, -0.5f, 1.0f, 0.0f, 1.0f, 1.0f,
            -0.5f, 0.5f, 1.0f, 0.0f, 1.0f, 1.0f,
            -0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 1.0f,
            -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 1.0f,

            0.5f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f,

            0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 1.0f,
            0.5f, -0.5f, 1.0f, 1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, 1.0f, 1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 1.0f,
    };

    public EntityRenderSystem(ViewMatrixResource viewMatrixResource) {
        this.viewMatrixResource = viewMatrixResource;

        String vertexSrc = null;
        String fragmentSrc = null;

        try {
            vertexSrc = File.readToString("vertex.glsl");
            fragmentSrc = File.readToString("fragment.glsl");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSrc);
        glCompileShader(vertexShader);

        System.out.println(glGetShaderInfoLog(vertexShader));

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSrc);
        glCompileShader(fragmentShader);

        System.out.println(glGetShaderInfoLog(fragmentShader));

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        System.out.println(glGetProgramInfoLog(program));

        this.program = program;

        this.colorLoc = glGetAttribLocation(program, "in_Color");
        this.vertexLoc = glGetAttribLocation(program, "in_Position");

        this.projULoc = glGetUniformLocation(program, "projMatrix");
        this.viewULoc = glGetUniformLocation(program, "viewMatrix");
        this.modelULoc = glGetUniformLocation(program, "modelMatrix");

        FloatBuffer verticesBuf = BufferUtils.createFloatBuffer(CUBE_VERTICES.length);
        verticesBuf.put(CUBE_VERTICES).flip();

        this.buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        glBufferData(GL_ARRAY_BUFFER, verticesBuf, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
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
                800.f / 600.f, 0.05f, 1000.0f);

        glUseProgram(program);

        glEnableVertexAttribArray(vertexLoc);
        glEnableVertexAttribArray(colorLoc);

        glBindBuffer(GL_ARRAY_BUFFER, buffer);

        glVertexAttribPointer(vertexLoc, 3, GL_FLOAT, false, 24, 0);
        glVertexAttribPointer(colorLoc, 3, GL_FLOAT, false, 24, 12);

        for (Entity entity : entities) {
            Position position = pm.get(entity);
            Rotation rotation = rm.get(entity);
            Scale scale = sm.get(entity);

            Matrix4f modelMatrix = new Matrix4f()
                    .translate(position.x, position.y, position.z)
                    .scale(scale.x, scale.y, scale.z)
                    .rotateZ((float) Math.toRadians(rotation.yaw))
                    .rotateX((float) Math.toRadians(rotation.roll))
                    .rotateY((float) Math.toRadians(rotation.pitch));

            glUniformMatrix4fv(projULoc, false, projMatrix.get(fb));
            glUniformMatrix4fv(viewULoc, false, viewMatrixResource.getViewMatrix().get(fb));
            glUniformMatrix4fv(modelULoc, false, modelMatrix.get(fb));

            glDrawArrays(GL_QUADS, 0, 24);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glDisableVertexAttribArray(vertexLoc);
        glDisableVertexAttribArray(colorLoc);

        glUseProgram(0);
    }
}
