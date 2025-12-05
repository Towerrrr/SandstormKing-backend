package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum TotalPlayerCountEnum {

    TWO(2, "battlefield-schedules/2players.csv"),
    FOUR(4, "battlefield-schedules/4players.csv"),
    SIX(6, "battlefield-schedules/6players.csv"),
    EIGHT(8, "battlefield-schedules/8players.csv");

    private final Integer value;

    private final String battlefieldSchedulePath;

    TotalPlayerCountEnum(Integer value, String battlefieldSchedulePath) {
        this.value = value;
        this.battlefieldSchedulePath = battlefieldSchedulePath;
    }

    public static TotalPlayerCountEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TotalPlayerCountEnum anEnum : TotalPlayerCountEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
