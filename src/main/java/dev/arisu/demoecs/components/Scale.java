package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class Scale implements Component {
    public float x;
    public float y;
    public float z;

    public Scale() {
        this(1.0f, 1.0f, 1.0f);
    }

    public Scale(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
