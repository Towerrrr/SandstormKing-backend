package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum StartEnum {

    HAND_ZONE_TOP("牌组/手牌顶", "HAND_ZONE_TOP"),
    HAND_ZONE_BOTTOM("牌组/手牌底", "HAND_ZONE_BOTTOM"),
    THIS_CARD("本卡", "THIS_CARD"),
    A_MAIN_DECK("A主牌堆", "A_MAIN_DECK"),
    B_MAIN_DECK("B主牌堆", "B_MAIN_DECK"),
    C_MAIN_DECK("C主牌堆", "C_MAIN_DECK");

    private final String text;

    private final String value;

    StartEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static StartEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (StartEnum anEnum : StartEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
