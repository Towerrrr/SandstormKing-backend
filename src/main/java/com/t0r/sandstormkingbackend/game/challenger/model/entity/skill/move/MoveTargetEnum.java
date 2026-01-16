package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MoveTargetEnum {

    OPPONENT("对手", "OPPONENT"),
    SELF("自己", "SELF");

    private final String text;

    private final String value;

    MoveTargetEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MoveTargetEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MoveTargetEnum anEnum : MoveTargetEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
