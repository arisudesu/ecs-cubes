package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.EntitySystem;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.terrain.Terrain;
import dev.arisu.demoecs.util.File;
import dev.arisu.demoecs.util.Pair;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
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
import static org.lwjgl.opengl.GL20.glClearColor;
import static org.lwjgl.opengl.GL20.glClearDepth;
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
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class TerrainRenderSystem extends EntitySystem {
    private final Terrain terrain;
    private final ViewMatrixResource viewMatrixResource;

    private int program;

    private int vertexLoc;
    private int colorLoc;

    private int projULoc, viewULoc, modelULoc;
    private int fogEnableULoc;
    private int fogDensityULoc;
    private int fogColorULoc;

    private final ArrayList<Pair<Integer, Integer>> renderQueue = new ArrayList<>();

    private final ArrayList<Pair<Integer, Integer>> chunks = new ArrayList<>();

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

        this.fogEnableULoc = glGetUniformLocation(program, "fogEnable");
        this.fogDensityULoc = glGetUniformLocation(program, "fogDensity");
        this.fogColorULoc = glGetUniformLocation(program, "fogColor");

        for (int x = -5; x < 5; ++x) {
            for (int y = -5; y < 5; ++y) {
                renderQueue.add(new Pair<>(x, y));
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        executeRenderQueue();

        glClearColor(0.5f, 0.8f, 1.0f, 0.0f);
        glClearDepth(1.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective(
                (float) Math.toRadians(70.0f),
                800.f / 600.f, 0.05f, 1000.0f);

        glUseProgram(program);

        glEnableVertexAttribArray(vertexLoc);
        glEnableVertexAttribArray(colorLoc);

        Matrix4f modelMatrix = new Matrix4f();

        glUniformMatrix4fv(projULoc, false, projMatrix.get(fb));
        glUniformMatrix4fv(viewULoc, false, viewMatrixResource.getViewMatrix().get(fb));
        glUniformMatrix4fv(modelULoc, false, modelMatrix.get(fb));

        glUniform1i(fogEnableULoc, 1);
        glUniform1f(fogDensityULoc, 0.01f);
        glUniform4f(fogColorULoc, 254f / 255f, 251f / 255f, 250f / 255f, 1.0f);

        for (Pair<Integer, Integer> chunk : chunks) {
            /// NOTE: `24` and `12` here are offsets in bytes
            glBindBuffer(GL_ARRAY_BUFFER, chunk.getA());

            glVertexAttribPointer(vertexLoc, 3, GL_FLOAT, false, 24, 0);
            glVertexAttribPointer(colorLoc, 3, GL_FLOAT, false, 24, 12);

            glDrawArrays(GL_QUADS, 0, chunk.getB() * 4);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        glDisableVertexAttribArray(vertexLoc);
        glDisableVertexAttribArray(colorLoc);

        glUseProgram(0);
    }

    private void executeRenderQueue() {

        if (renderQueue.isEmpty()) {
            return;
        }

        final Pair<Integer, Integer> chunkCoords = renderQueue.remove(0);
        final FloatBuffer verticesBuf =
                BufferUtils.createFloatBuffer(16 * 16 * 64 * 216);

        final int minX = chunkCoords.getA() * 16;
        final int minY = chunkCoords.getB() * 16;
        final int maxX = minX + 15;
        final int maxY = minY + 15;

        int faces = 0;

        for (int z = 0; z < 64; ++z) {
            for (int y = minY; y <= maxY; ++y) {
                for (int x = minX; x <= maxX; ++x) {

                    float[] vertices1 = new float[]{
                            x + 1.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.5f, 0.0f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.5f, 0.0f, 0.0f
                    };

                    float[] vertices2 = new float[]{
                            x + 1.0f, y + 0.0f, z + 1.0f, 0.25f, 0.125f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 1.0f, 0.25f, 0.125f, 0.0f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.25f, 0.125f, 0.0f,
                            x + 0.0f, y + 0.0f, z + 1.0f, 0.25f, 0.125f, 0.0f,
                    };

                    float[] vertices3 = new float[]{
                            x + 1.0f, y + 0.0f, z + 0.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 1.0f, z + 1.0f, 0.0f, 0.5f, 0.0f,
                            x + 1.0f, y + 0.0f, z + 1.0f, 0.0f, 0.5f, 0.0f,
                    };

                    float[] vertices4 = new float[]{
                            x + 0.0f, y + 0.0f, z + 1.0f, 0.5f, 0.5f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.5f, 0.5f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.5f, 0.5f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.5f, 0.5f, 0.5f,
                    };

                    float[] vertices5 = new float[]{
                            x + 1.0f, y + 1.0f, z + 1.0f, 0.0f, 0.0f, 0.5f,
                            x + 1.0f, y + 1.0f, z + 0.0f, 0.0f, 0.0f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 0.0f, 0.0f, 0.0f, 0.5f,
                            x + 0.0f, y + 1.0f, z + 1.0f, 0.0f, 0.0f, 0.5f,
                    };

                    float[] vertices6 = new float[]{
                            x + 1.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.5f,
                            x + 1.0f, y + 0.0f, z + 1.0f, 0.5f, 0.0f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 1.0f, 0.5f, 0.0f, 0.5f,
                            x + 0.0f, y + 0.0f, z + 0.0f, 0.5f, 0.0f, 0.5f,
                    };

                    if (!terrain.hasBlock(x, y, z)) {
                        continue;
                    }

                    if (!terrain.hasBlock(x, y, z - 1)) {
                        verticesBuf.put(vertices1);
                        faces++;
                    }

                    if (!terrain.hasBlock(x, y, z + 1)) {
                        verticesBuf.put(vertices2);
                        faces++;
                    }

                    if (!terrain.hasBlock(x + 1, y, z)) {
                        verticesBuf.put(vertices3);
                        faces++;
                    }

                    if (!terrain.hasBlock(x - 1, y, z)) {
                        verticesBuf.put(vertices4);
                        faces++;
                    }

                    if (!terrain.hasBlock(x, y + 1, z)) {
                        verticesBuf.put(vertices5);
                        faces++;
                    }

                    if (!terrain.hasBlock(x, y - 1, z)) {
                        verticesBuf.put(vertices6);
                        faces++;
                    }
                }
            }
        }

        verticesBuf.flip();

        final int newBuffer = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, newBuffer);
        glBufferData(GL_ARRAY_BUFFER, verticesBuf, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        chunks.add(new Pair<>(newBuffer, faces));
    }
}
