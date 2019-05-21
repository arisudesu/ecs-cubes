package dev.arisu.demoecs.systems;

import dev.arisu.demoecs.components.BoundingBox;
import dev.arisu.demoecs.components.Position;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class AABB {
    final float minX;
    final float minY;
    final float minZ;
    final float maxX;
    final float maxY;
    final float maxZ;

    public AABB(BoundingBox boundingBox, Position position) {
        this(
                position.x - boundingBox.width * 0.5f,
                position.y - boundingBox.depth * 0.5f,
                position.z,
                position.x + boundingBox.width * 0.5f,
                position.y + boundingBox.depth * 0.5f,
                position.z + boundingBox.height);
    }

    public float getHalfWidth() {
        return (maxX - minX) * 0.5f;
    }

    public float getHalfDepth() {
        return (maxY - minY) * 0.5f;
    }

    public float getHalfHeight() {
        return (maxZ - minZ) * 0.5f;
    }

    public AABB expand(float x, float y, float z) {
        float minX = this.minX;
        float minY = this.minY;
        float minZ = this.minZ;
        float maxX = this.maxX;
        float maxY = this.maxY;
        float maxZ = this.maxZ;

        if (x > 0.0f) {
            maxX += x;
        } else if (x < 0.0f) {
            minX += x;
        }

        if (y > 0.0f) {
            maxY += y;
        } else if (y < 0.0f) {
            minY += y;
        }

        if (z > 0.0f) {
            maxZ += z;
        } else if (z < 0.0f) {
            minZ += z;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean intersects(AABB a, AABB b) {
        return a.maxX > b.minX && a.minX < b.maxX &&
                a.maxY > b.minY && a.minY < b.maxY &&
                a.maxZ > b.minZ && a.minZ < b.maxZ;
    }
}
