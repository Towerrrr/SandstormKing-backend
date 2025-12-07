package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ChallengerMessageTypeEnum {

    INIT_GAME("初始化游戏", "INIT_GAME"),
    END_GAME("结束游戏", "END_GAME"),
    GET_PLAYER("获取玩家信息", "GET_PLAYER"),
    GET_BATTLEFIELD("获取战场信息", "GET_BATTLEFIELD"),
    BUILD_DECK("构建牌组", "BUILD_DECK"),
    READY_BATTLE("准备战斗", "READY_BATTLE"),
    DISCARD_CARD("弃牌", "DISCARD_CARD");

    private final String text;

    private final String value;

    ChallengerMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChallengerMessageTypeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChallengerMessageTypeEnum anEnum : ChallengerMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
