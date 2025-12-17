package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum TargetEnum {

    OPPONENT("对手", "OPPONENT"),
    SELF("自己", "SELF"),
    SELF_TO_OPPONENT("自己帮对手", "SELF_TO_OPPONENT");

    private final String text;

    private final String value;

    TargetEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static TargetEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TargetEnum anEnum : TargetEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
