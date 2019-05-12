package dev.arisu.demoecs;

public class TickCounter {
    public static final int RESOLUTION = 50_000_000;
    private long lastTickTime;
    private long ticks;

    public TickCounter(long current) {
        lastTickTime = current;
    }

    public void update(long current) {
        while (current - lastTickTime > RESOLUTION) {
            lastTickTime += RESOLUTION;
            ticks++;
        }
    }

    public long getTicks() {
        return ticks;
    }
}
