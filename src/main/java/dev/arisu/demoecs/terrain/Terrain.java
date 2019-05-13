package dev.arisu.demoecs.terrain;

import java.util.Random;

public class Terrain {

    private final byte[][][] blocks = new byte[256][256][128];

    public Terrain() {
        Random rand = new Random();
        for (int x = -128; x < 128; ++x) {
            for (int y = -128; y < 128; ++y) {
                for (int z = 0, zLimit = rand.nextInt(2); z < zLimit; ++z) {
                    setBlock(x, y, z);
                }
            }
        }
        setBlock(0, 0, 0);
    }

    public byte getBlock(int x, int y, int z) {
        if (x < -128 || x >= 128 || y < -128 || y >= 128 || z < 0 || z >= 128) {
            return 0;
        }
        return blocks[x + 128][y + 128][z];
    }

    public void setBlock(int x, int y, int z) {
        blocks[x + 128][y + 128][z] = 1;
    }
}
