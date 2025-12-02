package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ChallengerMessageTypeEnum {

    INIT_GAME("初始化游戏", "init_game"),
    END_GAME("结束游戏", "end_game"),
    REFRESH("刷新", "refresh"),
    DRAW_CARD("抽牌", "draw_card"),
    DRAW_AGAIN("再次抽牌", "draw_again"),
    CONFIRM_CHOICE("确认选择", "confirm_choice"),
    READY_BATTLE("准备战斗", "readyBattle");

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
