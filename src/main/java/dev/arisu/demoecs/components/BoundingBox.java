package dev.arisu.demoecs.components;


import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BoundingBox implements Component {
    public float width;
    public float depth;
    public float height;

    public BoundingBox() {
        this(0.0f, 0.0f, 0.0f);
    }

    public BoundingBox(float width, float depth, float height) {
        this.width = width;
        this.depth = depth;
        this.height = height;
    }
}
