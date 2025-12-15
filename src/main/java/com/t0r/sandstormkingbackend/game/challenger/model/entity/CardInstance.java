package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

// TODO 这个卡实例类是直接整个传来传去，还是用id存，用的时候再查
@Data
public class CardInstance {

    private Integer id;

    private Integer cardId;

}
