package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.EntitySystem;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.terrain.Terrain;
import dev.arisu.demoecs.util.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL20.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL20.GL_FLOAT;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_QUADS;
import static org.lwjgl.opengl.GL20.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBufferData;
import static org.lwjgl.opengl.GL20.glClear;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glDrawArrays;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGenBuffers;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class TerrainRenderSystem extends EntitySystem {
    private final Terrain terrain;
    private final ViewMatrixResource viewMatrixResource;

    private boolean reset = true;
    private int count = 0;

    private int program;
    private int vertexLoc;
    private int colorLoc;
    private int projULoc, viewULoc, modelULoc;
    private int buffer;

    public TerrainRenderSystem(Terrain terrain,
                               ViewMatrixResource viewMatrixResource) {
        this.terrain = terrain;
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

        this.buffer = glGenBuffers();
    }

    @Override
    public void update(float deltaTime) {

        if (reset) {
            reset = false;

            updateBuffer();
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective(
                (float) Math.toRadians(70.0f),
                800.f / 600.f, 0.05f, 1000.0f);

        glUseProgram(program);

        glEnableVertexAttribArray(vertexLoc);
        glEnableVertexAttribArray(colorLoc);

        glBindBuffer(GL_ARRAY_BUFFER, buffer);

        /// NOTE: `24` and `12` here are offsets in bytes
        glVertexAttribPointer(vertexLoc, 3, GL_FLOAT, false, 24, 0);
        glVertexAttribPointer(colorLoc, 3, GL_FLOAT, false, 24, 12);

        Matrix4f modelMatrix = new Matrix4f();

        glUniformMatrix4fv(projULoc, false, projMatrix.get(fb));
        glUniformMatrix4fv(viewULoc, false, viewMatrixResource.getViewMatrix().get(fb));
        glUniformMatrix4fv(modelULoc, false, modelMatrix.get(fb));

        glDrawArrays(GL_QUADS, 0, 24 * count);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glDisableVertexAttribArray(vertexLoc);
        glDisableVertexAttribArray(colorLoc);

        glUseProgram(0);
    }

    private void updateBuffer() {
        FloatBuffer verticesBuf = BufferUtils.createFloatBuffer(256 * 256 * 64 * 72);

        for (int z = 0; z < 64; ++z) {
            for (int y = -128; y < 128; ++y) {
                for (int x = -128; x < 128; ++x) {
                    byte hasBlock = terrain.getBlock(x, y, z);

                    if (hasBlock == 0) {
                        continue;
                    }

                    float[] vertices = new float[]{
                            x + 1.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,

                            x + 1.0f, y + 0.0f, z + 1.0f, 0.5f, 0.5f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 1.0f, 0.5f, 0.5f, 0.0f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.5f, 0.5f, 0.0f,
                            x + 0.0f, y + 0.0f, z + 1.0f, 0.5f, 0.5f, 0.0f,

                            x + 1.0f, y + 0.0f, z + 0.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 1.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 0.0f, z + 1.0f, 0.0f, 0.5f, 0.0f,

                            x + 0.0f, y + 0.0f, z + 1.0f, 0.0f, 0.5f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.0f, 0.5f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.0f, 0.5f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.0f, 0.5f, 0.5f,

                            x + 1.0f, y + 1.0f, z + 1.0f, 0.0f, 0.0f, 0.5f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.0f, 0.0f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.0f, 0.0f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.0f, 0.0f, 0.5f,

                            x + 1.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.5f,
                            x + 1.0f, y + 0.0f, z + 1.0f, 0.5f, 0.0f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 1.0f, 0.5f, 0.0f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.5f,
                    };

                    verticesBuf.put(vertices);
                    ++count;
                }
            }
        }

        verticesBuf.flip();

        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        glBufferData(GL_ARRAY_BUFFER, verticesBuf, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
}
