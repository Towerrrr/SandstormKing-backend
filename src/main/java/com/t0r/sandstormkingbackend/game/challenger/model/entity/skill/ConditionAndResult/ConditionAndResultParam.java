package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult;

import lombok.Data;

@Data
public class ConditionAndResultParam {

    private ConditionEnum conditionEnum;

    /**
     * 牌组、
     */
    private String commonParam;

    private ResultEnum resultEnum;

    /**
     * PER_，每...，取 1 / -1
     * 其他的种类正常加
     */
    private int resultIncrement;

}
