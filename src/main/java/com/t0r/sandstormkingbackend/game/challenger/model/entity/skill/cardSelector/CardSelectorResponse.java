package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import lombok.Data;

import java.util.Set;

/**
 * 前端对服务器的相应
 */
@Data
public class CardSelectorResponse {

    private String userId;

    /**
     * null 必定触发的技能，
     * true 玩家选择触发技能，
     * false 玩家选择不触发技能。
     */
    Boolean isTrigger = null;

    // TODO 目前先实现 1 张卡的技能
    private Integer selectedCardInstanceId;

}
