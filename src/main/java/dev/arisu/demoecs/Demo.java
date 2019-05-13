package dev.arisu.demoecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import dev.arisu.demoecs.components.*;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.systems.*;
import dev.arisu.demoecs.terrain.Terrain;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Demo {

    // The window handle
    private long window;
    private TickCounter tickCounter;
    private InputState inputState;
    private Engine engine;

    private void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        glfwShowWindow(window);
    }

    private void loop() {
        GL.createCapabilities();

        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        glClearDepth(1.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        engine = new Engine();

        for (int eIndex = 0; eIndex < 10; ++eIndex) {
            Entity entity = new Entity();
            entity.add(new Position(5 * eIndex, 0.0f, 0.0f));
            entity.add(new Rotation());
            entity.add(new Scale(0.6f, 0.6f, 1.75f));
            entity.add(new BoundingBox(0.6f, 0.6f, 1.75f));

            engine.addEntity(entity);
        }

        Entity player = new Entity();
        player.add(new Position(0.0f, 0.0f, 0.0f));
        player.add(new Rotation());
        player.add(new Scale());
        player.add(new PlayerTag());
        player.add(new BoundingBox(0.6f, 0.6f, 1.75f));

        engine.addEntity(player);

        tickCounter = new TickCounter(System.nanoTime());
        inputState = new InputState();

        glfwSetCursorPos(window, 0, 0);

        glfwSetWindowSizeCallback(window, (window1, width, height) -> {
            glfwSetCursorPos(window, 0.0, 0.0);
        });
        glfwSetKeyCallback(window, (l, i, i1, i2, i3) -> {
            boolean isPressed = i2 == GLFW_PRESS || i2 == GLFW_REPEAT;

            if (i == GLFW_KEY_ESCAPE && !isPressed) {
                glfwSetWindowShouldClose(window, true);
                return;
            }

            if (i == GLFW_KEY_W) {
                inputState.setW(isPressed);
            } else if (i == GLFW_KEY_S) {
                inputState.setS(isPressed);
            } else if (i == GLFW_KEY_A) {
                inputState.setA(isPressed);
            } else if (i == GLFW_KEY_D) {
                inputState.setD(isPressed);
            } else if (i == GLFW_KEY_SPACE) {
                inputState.setSpace(isPressed);
            }
        });
        glfwSetWindowSizeCallback(window, (window1, width, height) -> {
            glViewport(0, 0, width, height);
        });
        glViewport(0, 0, 800, 600);

        ArrayBlockingQueue<MoveSystem.MouseMove> mouseMoves =
                new ArrayBlockingQueue<>(9999);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetCursorPosCallback(window, (window1, xpos, ypos) -> {
            mouseMoves.offer(new MoveSystem.MouseMove(xpos, ypos));
            glfwSetCursorPos(window, 0, 0);
        });

        ViewMatrixResource viewMatrixResource = new ViewMatrixResource();

        Terrain terrain = new Terrain();

        engine.addSystem(new GravitySystem(terrain));
        engine.addSystem(new MoveSystem(inputState, mouseMoves));
        engine.addSystem(new ViewMatrixUpdateSystem(viewMatrixResource));
        engine.addSystem(new TerrainRenderSystem(terrain, viewMatrixResource));
        engine.addSystem(new EntityRenderSystem(viewMatrixResource));

        while (!glfwWindowShouldClose(window)) {

            runGameStep();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void runGameStep() {

        tickCounter.update(System.nanoTime());
        engine.update(0.1f);
    }

    public static void main(String[] args) {
        new Demo().run();
    }

}