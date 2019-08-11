package dev.arisu.demoecs.terrain;

import java.util.Random;

public class Terrain {

    private final byte[][][] blocks = new byte[256][256][128];

    public Terrain() {
        Random rand = new Random();
        for (int x = -128; x < 128; ++x) {
            for (int y = -128; y < 128; ++y) {

                setBlock(x, y, 0);

                if (rand.nextInt(15) != 0) {
                    continue;
                }

                for (int z = 1, zLimit = rand.nextInt(4); z < zLimit; ++z) {
                    setBlock(x, y, z);
                }
            }
        }
        resetBlock(0, 0, 1);
        resetBlock(0, 0, 2);

        setBlock(2, 2, 1);
        setBlock(2, 2, 2);
        setBlock(2, 2, 3);
        setBlock(2, 2, 4);

        setBlock(4, 2, 1);
        setBlock(4, 2, 2);
        setBlock(4, 2, 3);
        setBlock(4, 2, 4);

        setBlock(3, 2, 1);
        setBlock(3, 2, 4);

        resetBlock(3, 2, 2);
        resetBlock(3, 2, 3);
    }

    public byte getBlock(int x, int y, int z) {
        if (x < -128 || x >= 128 || y < -128 || y >= 128 || z < 0 || z >= 128) {
            return 0;
        }
        return blocks[x + 128][y + 128][z];
    }

    public boolean hasBlock(int x, int y, int z) {
        return getBlock(x, y, z) != 0;
    }

    public void setBlock(int x, int y, int z) {
        blocks[x + 128][y + 128][z] = 1;
    }

    public void resetBlock(int x, int y, int z) {
        blocks[x + 128][y + 128][z] = 0;
    }
}
