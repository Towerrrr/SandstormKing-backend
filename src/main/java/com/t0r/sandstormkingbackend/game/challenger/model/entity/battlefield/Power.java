package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Power {

    int tempValue;

    /**
     * 用这个值攻受互换
     */
    int value;

    public Power(int value) {
        this.value = value;
        this.tempValue = value;
    }

    public void addBase(int value) {
        this.value += value;
        this.tempValue += value;
    }

    public void addTemp(int value) {
        this.tempValue += value;
    }

}
