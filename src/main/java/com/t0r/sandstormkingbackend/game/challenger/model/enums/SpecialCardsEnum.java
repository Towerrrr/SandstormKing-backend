package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpecialCardsEnum {

    MACHINE("基础力量值等于当前回合数", "机械");
    // TODO 鹿娃


    private final String text;

    private final String name;

    SpecialCardsEnum(String text, String name) {
        this.text = text;
        this.name = name;
    }

    public static SpecialCardsEnum getByName(String name) {
        if (ObjUtil.isEmpty(name)) {
            return null;
        }
        for (SpecialCardsEnum anEnum : SpecialCardsEnum.values()) {
            if (anEnum.name.equals(name)) {
                return anEnum;
            }
        }
        return null;
    }
}
