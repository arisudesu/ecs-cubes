package dev.arisu.demoecs.terrain;

import java.util.Random;

public class Terrain {

    private final byte[][][] blocks = new byte[256][256][128];

    public Terrain() {
        Random rand = new Random();
        for (int x = 0; x < 256; ++x) {
            for (int y = 0; y < 256; ++y) {
                for (int z = 0, zLimit = rand.nextInt(3); z < zLimit; ++z) {
                    setBlock(x, y, z);
                }
            }
        }
        setBlock(0, 0, 0);
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= 256 || y < 0 || y >= 256 || z < 0 || z >= 128) {
            return 0;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z) {
        blocks[x][y][z] = 1;
    }
}
