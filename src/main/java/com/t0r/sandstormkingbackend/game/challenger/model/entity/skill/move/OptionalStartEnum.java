package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum OptionalStartEnum {

    REST_ZONE("休息区", "REST_ZONE"),
    CONSUMED_DECK("消耗牌堆", "CONSUMED_DECK"),
    HAND_ZONE("牌组/手牌", "HAND_ZONE");

    private final String text;

    private final String value;

    OptionalStartEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static OptionalStartEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OptionalStartEnum anEnum : OptionalStartEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
