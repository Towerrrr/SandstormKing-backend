package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MoveTypeEnum {

    CARD_INSTANCE("卡实例", "CARD_INSTANCE"),
    SLOT("位置", "SLOT"),
    LOWEST_POWER_CARD_INSTANCE("力量最低的卡实例", "LOWEST_POWER_CARD_INSTANCE");

    private final String text;

    private final String value;

    MoveTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MoveTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MoveTypeEnum anEnum : MoveTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
