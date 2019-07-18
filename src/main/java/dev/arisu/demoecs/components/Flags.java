package dev.arisu.demoecs.components;

import com.badlogic.ashley.core.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Flags implements Component {
    boolean onGround;
}
