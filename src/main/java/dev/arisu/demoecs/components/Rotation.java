package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Rotation implements Component {
    public float roll; // x
    public float pitch; // y
    public float yaw; // z

    public Rotation() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Rotation(float roll, float pitch, float yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }
}
