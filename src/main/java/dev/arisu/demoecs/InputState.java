package dev.arisu.demoecs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class InputState {
    public boolean w, s, a, d;
    public boolean space;
}
