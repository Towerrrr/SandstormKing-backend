package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ComparisonOperatorEnum {

    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUAL("="),
    GREATER_EQUAL(">="),
    LESS_EQUAL("<=");

    private final String value;

    ComparisonOperatorEnum(String value) {
        this.value = value;
    }

    public static ComparisonOperatorEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ComparisonOperatorEnum anEnum : ComparisonOperatorEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
