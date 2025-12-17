package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffConfigParam;
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

    private String timeRange = null;

    private String buffType = null;

    private BuffConfigParam buffConfigParam = null;

}
