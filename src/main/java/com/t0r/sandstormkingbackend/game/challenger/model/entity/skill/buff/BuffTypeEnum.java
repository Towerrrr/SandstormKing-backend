package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum BuffTypeEnum {

    REST("在休息区", "REST"),
    NEXT("下一张卡", "NEXT");

    private final String text;

    private final String value;

    BuffTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static BuffTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (BuffTypeEnum anEnum : BuffTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
