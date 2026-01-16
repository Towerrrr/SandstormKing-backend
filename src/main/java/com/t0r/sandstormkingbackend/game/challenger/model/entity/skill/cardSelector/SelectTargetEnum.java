package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SelectTargetEnum {

    OPPONENT("对手", "OPPONENT"),
    SELF("自己", "SELF");

    private final String text;

    private final String value;

    SelectTargetEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static SelectTargetEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SelectTargetEnum anEnum : SelectTargetEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
