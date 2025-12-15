package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Power {

    int value;

    public void add(int value) {
        this.value += value;
    }

}
