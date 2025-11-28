package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum RoundEnum {

    ONE("1", "1"),
    TWO("2", "2"),
    THREE("3", "3"),
    FOUR("4", "4"),
    FIVE("5", "5"),
    SIX("6", "6"),
    SEVEN("7", "7"),
    FINAL("final", "final");
//    TODO 友谊赛

    private final String text;

    private final String value;

    RoundEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public RoundEnum getNextRound() {
        int index = this.ordinal();
        if (index >= RoundEnum.values().length - 1) {
            return null;
        }
        return RoundEnum.values()[index + 1];
    }

    public static RoundEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (RoundEnum anEnum : RoundEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public static RoundEnum getFirstRound() {
        return RoundEnum.values()[0];
    }

    public static RoundEnum getLastRound() {
        return RoundEnum.values()[RoundEnum.values().length - 1];
    }

}
