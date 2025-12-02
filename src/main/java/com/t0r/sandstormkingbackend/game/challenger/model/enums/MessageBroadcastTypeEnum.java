package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum MessageBroadcastTypeEnum {

    ALL("全体", "all"),
    OTHERS("除自己外", "others"),
    SELF("自己", "self"),
    CUSTOM("自定义", "custom");

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
