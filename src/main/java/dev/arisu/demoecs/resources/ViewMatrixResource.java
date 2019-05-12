package dev.arisu.demoecs.resources;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;

@Getter
@Setter
public class ViewMatrixResource {
    private Matrix4f viewMatrix;
}
