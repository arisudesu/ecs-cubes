package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.EntitySystem;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.terrain.Terrain;
import dev.arisu.demoecs.util.File;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.*;

public class TerrainRenderSystem extends EntitySystem {
    private final Terrain terrain;
    private final ViewMatrixResource viewMatrixResource;

    private final static float WIDTH = 1.0f;

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
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (reset) {
            reset = false;

            updateBuffer();
        }

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
                    count++;
                }
            }
        }

        verticesBuf.flip();

        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        glBufferData(GL_ARRAY_BUFFER, verticesBuf, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /*@Deprecated
    public void update_disable(float deltaTime) {

        if (reset) {
            System.out.println("world has been reset!");

            buf = BufferUtils.createFloatBuffer(256 * 256 * 64 * 72);
            pointer = 0;

            for (int z = 0; z < 64; ++z) {
                for (int y = -128; y < 128; ++y) {
                    for (int x = -128; x < 128; ++x) {
                        byte hasBlock = terrain.getBlock(x, y, z);

                        if (hasBlock != 0) {
                            pointer++;

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x + WIDTH);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z);

                            buf.put(x);
                            buf.put(y + WIDTH);
                            buf.put(z + WIDTH);

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z);

                            buf.put(x + WIDTH);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z + WIDTH);

                            buf.put(x);
                            buf.put(y);
                            buf.put(z);
                        }
                    }
                }
            }

            buf.flip();
            reset = false;

            listId = glGenLists(1);
            glNewList(1, GL_COMPILE);

            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(3, GL_FLOAT, 0, buf);

            glColor3f(0.0f, 0.0f, 0.5f);
            glDrawArrays(GL_QUADS, 0, pointer * 24);

            System.out.println(pointer * 24);

//            glColor3f(0.0f, 0.0f, 1.0f);
//            glDrawArrays(GL_LINES, 0, pointer * 24 * 3);

            glDisableClientState(GL_VERTEX_ARRAY);

            glEndList();
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glMatrixMode(GL_MODELVIEW);

        Matrix4f modelMatrix = new Matrix4f()
                .translate(0.0f, 0.0f, 0.0f);
        Matrix4f modelViewMatrix = new Matrix4f();

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        glLoadMatrixf(viewMatrixResource.getViewMatrix().mul(modelMatrix, modelViewMatrix).get(fb));

//        glCallList(listId);

//        glEnableClientState(GL_VERTEX_ARRAY);
//        glVertexPointer(3, GL_FLOAT, 0, buf);
//
//        glColor3f(0.0f, 0.0f, 0.5f);
//        glDrawArrays(GL_QUADS, 0, pointer * 24);
//
//        glColor3f(0.0f, 0.0f, 1.0f);
//        glDrawArrays(GL_LINES, 0, pointer * 24 * 3);
//
//        glDisableClientState(GL_VERTEX_ARRAY);

        glFlush();
    }*/
}
