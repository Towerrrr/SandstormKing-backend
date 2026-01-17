package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter;

// TODO 后端收到不符合的卡牌过滤条件时，返回错误信息，前端重新选择
public class CardFilter {

    private String name;

    private String compareOperator;

    private Integer basePower;

    private String group;

    private String level;

}
