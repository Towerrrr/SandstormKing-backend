package com.t0r.sandstormkingbackend.model.enums;

import lombok.Getter;

@Getter
public enum PlayerActionEnum {

    // todo 后续补充
    // 抽 A 卡，抽 B 卡，抽 C 卡
    DRAW_A("抽 A 卡", "DRAW_A"),
    DRAW_B("抽 B 卡", "DRAW_B"),
    DRAW_C("抽 C 卡", "DRAW_C");

    private final String text;
    private final String value;

    PlayerActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PlayerActionEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PlayerActionEnum actionEnum : PlayerActionEnum.values()) {
            if (actionEnum.value.equals(value)) {
                return actionEnum;
            }
        }
        return null;
    }
}

