#version 120

varying vec3 ex_Color;

uniform bool fogEnable = false;
uniform float fogDensity = 1.0;
uniform float fogStart = 0.0;
uniform float fogEnd = 1.0;
uniform vec4 fogColor = vec4(0.0);

float fogFactor() {
    float fragDepth = gl_FragCoord.z / gl_FragCoord.w;
    float fogFactor;
    fogFactor = exp(-(fogDensity * fragDepth));
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    return fogFactor;
}

void main()
{
    if (fogEnable) {
        gl_FragColor = mix(fogColor, vec4(ex_Color, 1.0), fogFactor());
    } else {
        gl_FragColor = vec4(ex_Color, 1.0);
    }
}
