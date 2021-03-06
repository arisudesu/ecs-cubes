#version 120

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projMatrix;

attribute vec3 in_Position;
attribute vec3 in_Color;

varying vec3 ex_Color;

void main()
{
    gl_Position = projMatrix * viewMatrix * modelMatrix * vec4(in_Position, 1.0);
    ex_Color = in_Color;
}
