package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Position implements Component {
    public float x;
    public float y;
    public float z;

    public Position() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Position(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
