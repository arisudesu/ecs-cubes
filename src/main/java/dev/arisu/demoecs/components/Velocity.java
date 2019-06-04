package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class Velocity implements Component {
    public float x;
    public float y;
    public float z;

    public Velocity() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Velocity(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
