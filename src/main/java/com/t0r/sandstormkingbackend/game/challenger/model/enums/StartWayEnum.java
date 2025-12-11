package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum StartWayEnum {

    NORMAL("正常", "NORMAL"),
    RANDOM("随机", "RANDOM");

    private final String text;

    private final String value;

    StartWayEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static StartWayEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (StartWayEnum anEnum : StartWayEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
