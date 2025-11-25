package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum BattlefieldEnum {

    GREEN("绿色", "green"),
    RED("红色", "red"),
    PURPLE("紫色", "purple"),
    YELLOW("黄色", "yellow");

    private final String text;

    private final String value;

    BattlefieldEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static BattlefieldEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (BattlefieldEnum anEnum : BattlefieldEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
