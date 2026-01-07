package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult;

public enum ConditionEnum {

    PREVIOUS_MATCH_LOST, // 上一场比赛输了

    HAS_CARD_UNDERNEATH, // 这张卡下面有卡
    HAND_NEARLY_EMPTY, // 手牌区剩 0 或 1 张卡
    PER_CONSUMED_CARD, // 消耗牌堆每有 1 张卡
    OPPONENT_REST_HAS_ROOKIE, // 对手休息区有新丁
    OPPONENT_CONSUMED_NOT_EMPTY, // 对手消耗牌堆非空
    OPPONENT_PER_CONSUMED_CARD, // 对手消耗牌堆每有 1 张卡

    OPPONENT_PER_HAS_CUP, // 对手每有一个奖杯

}
