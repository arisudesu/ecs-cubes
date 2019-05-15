#version 120

varying vec3 ex_Color;
varying vec3 ex_Normal;
varying vec3 ex_FragPos;

uniform bool fogEnable = false;
uniform float fogDensity = 1.0;
uniform float fogStart = 0.0;
uniform float fogEnd = 1.0;
uniform vec4 fogColor = vec4(0.0);

uniform bool lightEnable = true;
uniform vec3 lightPosition = vec3(0.0, 0.0, 10.0);
uniform vec3 lightColor = vec3(1.0);

float fogFactor() {
    float fragDepth = gl_FragCoord.z / gl_FragCoord.w;
    float fogFactor;
    fogFactor = exp(-(fogDensity * fragDepth));
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    return fogFactor;
}

void main() {
    vec3 norm = normalize(ex_Normal);
    vec3 lightDir = normalize(lightPosition - ex_FragPos);

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    vec4 lightedFragColor = vec4(diffuse * ex_Color, 1.0);

    if (fogEnable) {
        gl_FragColor = mix(fogColor, lightedFragColor, fogFactor());
    } else {
        gl_FragColor = lightedFragColor;
    }
}
