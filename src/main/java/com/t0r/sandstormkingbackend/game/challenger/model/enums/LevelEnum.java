package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum LevelEnum {

    S("S", false),
    A("A", true),
    B("B", true),
    C("C", true);

    private final String value;

    private final boolean isKept;

    LevelEnum(String value, boolean isKept) {
        this.value = value;
        this.isKept = isKept;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static LevelEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (LevelEnum anEnum : LevelEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
