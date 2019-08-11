package dev.arisu.demoecs.systems;

enum Normal {
    UP(0.0f, 0.0f, 1.0f),
    DOWN(0.0f, 0.0f, -1.0f),
    NORTH(0.0f, 1.0f, 0.0f),
    SOUTH(0.0f, -1.0f, 0.0f),
    WEST(-1.0f, 0.0f, 0.0f),
    EAST(1.0f, 0.0f, 0.0f);

    public float x;
    public float y;
    public float z;

    Normal(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
