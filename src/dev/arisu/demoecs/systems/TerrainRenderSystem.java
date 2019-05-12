package dev.arisu.demoecs.systems;

import com.badlogic.ashley.core.EntitySystem;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.terrain.Terrain;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TerrainRenderSystem extends EntitySystem {

    private final Terrain terrain;
    private final ViewMatrixResource viewMatrixResource;

    private final static float WIDTH = 1.0f;

    private boolean reset = true;
    FloatBuffer buf;
    int pointer;

    public TerrainRenderSystem(Terrain terrain,
                               ViewMatrixResource viewMatrixResource) {
        this.terrain = terrain;
        this.viewMatrixResource = viewMatrixResource;
    }

    @Override
    public void update(float deltaTime) {
        if (reset) {
            System.out.println("world has been reset!");

            buf = BufferUtils.createFloatBuffer(256 * 256 * 64 * 72);
            pointer = 0;

            for (int z = 0; z < 64; ++z) {
                for (int y = 0; y < 256; ++y) {
                    for (int x = 0; x < 256; ++x) {
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
        }

        glMatrixMode(GL_MODELVIEW);

        Matrix4f modelMatrix = new Matrix4f()
                .translate(0.0f, 0.0f, 0.0f);
        Matrix4f modelViewMatrix = new Matrix4f();

        FloatBuffer fb = BufferUtils.createFloatBuffer(16);

        glLoadMatrixf(viewMatrixResource.getViewMatrix().mul(modelMatrix, modelViewMatrix).get(fb));


        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(3, GL_FLOAT, 0, buf);

        glColor3f(0.0f, 0.0f, 0.5f);
        glDrawArrays(GL_QUADS, 0, pointer * 24);

        glColor3f(0.0f, 0.0f, 1.0f);
        glDrawArrays(GL_LINES, 0, pointer * 24 * 3);

        glDisableClientState(GL_VERTEX_ARRAY);

        glFlush();
    }
}
