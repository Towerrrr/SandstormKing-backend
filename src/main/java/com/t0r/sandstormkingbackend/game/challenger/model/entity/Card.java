package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

@Data
public class Card {

    private Integer id;

    private String name;

    private Integer basePower;

    private String group;

    private String level;

    // todo 前端对特殊描述的渲染
    private String description;

    private Integer count;

}
