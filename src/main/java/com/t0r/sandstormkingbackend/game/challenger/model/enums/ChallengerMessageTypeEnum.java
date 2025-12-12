package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ChallengerMessageTypeEnum {

    INIT_GAME("初始化游戏", "INIT_GAME"),
    END_GAME("结束游戏", "END_GAME"),
    GET_PLAYER("获取玩家信息", "GET_PLAYER"),
    GET_BATTLEFIELD("获取战场信息", "GET_BATTLEFIELD"),
    GET_ROOM_STATE("获取房间状态", "GET_ROOM_STATE"),
    BUILD_DECK("构建牌组", "BUILD_DECK"),
    DISCARD_CARD("弃牌", "DISCARD_CARD"),
    // 仅接收
    READY_BATTLE("准备战斗", "READY_BATTLE"),
    // 仅发送
    WAIT_OPPONENT_READY("等待对手准备", "WAIT_OPPONENT_READY"),
    WAIT_YOU_READY("等待你准备", "WAIT_YOU_READY"),
    START_BATTLE("开始战斗", "START_BATTLE"),
    // TODO 未实现
    AWARD_AND_NEXT_ROUND("颁奖并进入下一回合", "AWARD_AND_NEXT_ROUND");

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
