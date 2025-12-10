package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PhaseEnum {

    BATTLE("战斗", "BATTLE"),
    BUILD("构筑", "BUILD");

    private final String text;

    private final String value;

    PhaseEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static PhaseEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PhaseEnum anEnum : PhaseEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
