package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import lombok.Data;

import java.util.*;
import java.util.function.Consumer;

@Data
public class HalfBattlefield {

    // 战斗前构筑是否就绪
    private boolean isReady = false;

    private LinkedList<CardInstance> handZone = new LinkedList<>();

    // 卡牌名称 -> 卡牌实例列表
    public final static int MAX_REST_ZONE_SIZE = 6;
    private Map<String, LinkedList<CardInstance>> restZone = new HashMap<>();

    // 消耗牌堆
    private List<CardInstance> consumedDeck = new ArrayList<>();

    // 休息区 BUFF
    List<Consumer<BuffCallParam>> restBuffs = new ArrayList<>();
    // 下一张卡 BUFF
    Consumer<BuffCallParam> nextBuff = null;

}
