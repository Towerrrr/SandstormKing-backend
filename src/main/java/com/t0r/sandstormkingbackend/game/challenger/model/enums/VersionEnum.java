package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum VersionEnum {

    VERSION_1("第一部", "version1", "draw-schedules/version-1"),
    VERSION_2("第二部", "version2", "draw-schedules/version-2");

    private final String text;

    private final String value;

    private final String drawSchedulesPath;

    VersionEnum(String text, String value, String drawSchedulesPath) {
        this.text = text;
        this.value = value;
        this.drawSchedulesPath = drawSchedulesPath;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static VersionEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (VersionEnum anEnum : VersionEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
