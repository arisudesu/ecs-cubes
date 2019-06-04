package dev.arisu.demoecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import dev.arisu.demoecs.components.BoundingBox;
import dev.arisu.demoecs.components.PlayerTag;
import dev.arisu.demoecs.components.Position;
import dev.arisu.demoecs.components.Rotation;
import dev.arisu.demoecs.components.Scale;
import dev.arisu.demoecs.resources.ViewMatrixResource;
import dev.arisu.demoecs.systems.EntityRenderSystem;
import dev.arisu.demoecs.systems.GravitySystem;
import dev.arisu.demoecs.systems.MoveSystem;
import dev.arisu.demoecs.systems.TerrainRenderSystem;
import dev.arisu.demoecs.systems.ViewMatrixUpdateSystem;
import dev.arisu.demoecs.terrain.Terrain;
import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import org.lwjgl.Version;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL20.GL_CULL_FACE;
import static org.lwjgl.opengl.GL20.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL20.glClearColor;
import static org.lwjgl.opengl.GL20.glClearDepth;
import static org.lwjgl.opengl.GL20.glEnable;
import static org.lwjgl.opengl.GL20.glViewport;
import org.lwjgl.system.MemoryStack;
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
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

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

        engine = new Engine();

        for (int eIndex = 0; eIndex < 10; ++eIndex) {
            Entity entity = new Entity();
            entity.add(new Position(5 * eIndex, 0.0f, 15.0f));
            entity.add(new Rotation());
            entity.add(new Scale(0.6f, 0.6f, 1.75f));
            entity.add(new BoundingBox(0.6f, 0.6f, 1.75f));

            engine.addEntity(entity);
        }

        Entity player = new Entity();
        player.add(new Position(0.0f, 0.0f, 15.0f));
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
