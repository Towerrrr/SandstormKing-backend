package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HalfBattlefield {

    private List<CardInstance> handZone = new ArrayList<>();

    // TODO 休息区 6 个的限制
    private List<CardInstance> restZone = new ArrayList<>();

    // 消耗牌堆
    private List<CardInstance> consumedDeck = new ArrayList<>();

}
