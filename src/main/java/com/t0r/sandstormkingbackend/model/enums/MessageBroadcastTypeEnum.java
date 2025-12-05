package com.t0r.sandstormkingbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MessageBroadcastTypeEnum {

    ALL("全体", "ALL"),
    OTHERS("除自己外", "OTHERS"),
    SELF("自己", "SELF"),
    CUSTOM("自定义", "CUSTOM");

    private final String text;

    private final String value;

    MessageBroadcastTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MessageBroadcastTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MessageBroadcastTypeEnum anEnum : MessageBroadcastTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
