package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SkillTypeEnum {

    CONDITION_AND_RESULT("条件后结果", "CONDITION_AND_RESULT"),
    SELECTOR_AND_MOVE("选择后移动", "SELECTOR_AND_MOVE"),
    CHECK_AND_MOVE_RESULT("查看后移动或结果", "CHECK_AND_MOVE_RESULT"),
    IMMEDIATELY_MOVE("立即移动", "IMMEDIATELY_MOVE");

    private final String text;

    private final String value;

    SkillTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static SkillTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SkillTypeEnum anEnum : SkillTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
