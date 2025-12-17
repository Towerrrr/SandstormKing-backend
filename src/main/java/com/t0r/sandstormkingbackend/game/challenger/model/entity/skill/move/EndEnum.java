package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum EndEnum {

    HAND_ZONE_TOP("牌组/手牌顶", "HAND_ZONE_TOP"),
    HAND_ZONE_BOTTOM("牌组/手牌底", "HAND_ZONE_BOTTOM"),
    REST_ZONE("休息区", "REST_ZONE"),
    CONSUMED_DECK("消耗牌堆", "CONSUMED_DECK"),
    DISCARD_DECK("弃牌堆", "DISCARD_DECK");

    private final String text;

    private final String value;

    EndEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static EndEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (EndEnum anEnum : EndEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
