package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class Movement implements Component {
    public float x;
    public float y;
    public float z;

    public Movement() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Movement(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
