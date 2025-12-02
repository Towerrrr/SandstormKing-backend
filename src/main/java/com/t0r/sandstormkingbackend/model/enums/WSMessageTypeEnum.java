package com.t0r.sandstormkingbackend.model.enums;

import lombok.Getter;

@Getter
public enum WSMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ROOM_STATE_CHANGED("房间状态变更", "ROOM_STATE_CHANGED"),
    START_GAME("开始游戏", "START_GAME"),
    CHALLENGER("挑战者", "CHALLENGER");

    private final String text;
    private final String value;

    WSMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static WSMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (WSMessageTypeEnum typeEnum : WSMessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}

