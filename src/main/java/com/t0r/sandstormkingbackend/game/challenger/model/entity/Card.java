package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult.ConditionAndResultParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffConfigParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPutParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveConfigParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveTargetEnum;
import lombok.Data;

@Data
public class Card {

    private Integer id;

    private String name;

    private Integer basePower;

    private String group;

    private String level;

    // todo 前端对特殊描述的渲染
    private String skillDescription;

    private String countDescription;

    /**
     * “S”卡为初始手牌数量，其他卡为在主牌库的数量
     */
    private Integer count = 4;

    // region 技能属性

    private String timeRange = null;

    // TODO 所有技能判断的地方进行修改
    private String skillType = null;

    private CheckAndPutParam checkAndPutParam = null;

    private ConditionAndResultParam conditionAndResultParam = null;

    private MoveTargetEnum moveTargetEnum = MoveTargetEnum.SELF;

    private MoveConfigParam moveConfigParam = null;

    private String buffType = null;

    private BuffConfigParam buffConfigParam = null;

    private CardSelectorParam cardSelectorParam = null;

}
