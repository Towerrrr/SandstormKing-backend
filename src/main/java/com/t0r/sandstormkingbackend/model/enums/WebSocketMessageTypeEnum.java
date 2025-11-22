package com.t0r.sandstormkingbackend.model.enums;

import lombok.Getter;

@Getter
public enum WebSocketMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ROOM_STATE_CHANGED("房间状态变更", "ROOM_STATE_CHANGED"),
    START_GAME("开始游戏", "START_GAME"),
    GAME_STATE("游戏状态更新", "GAME_STATE"),
    GAME_OVER("游戏结束", "GAME_OVER"),
    PLAYER_ACTION("玩家操作", "PLAYER_ACTION");

    private final String text;
    private final String value;

    WebSocketMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static WebSocketMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (WebSocketMessageTypeEnum typeEnum : WebSocketMessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}

