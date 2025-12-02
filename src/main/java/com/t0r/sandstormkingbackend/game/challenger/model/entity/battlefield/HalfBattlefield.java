package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;

import java.util.*;

@Data
public class HalfBattlefield {

    public final static int MAX_REST_ZONE_SIZE = 6;

    // 战斗前构筑是否就绪
    private boolean isReady = false;

    private LinkedList<CardInstance> handZone = new LinkedList<>();

    // TODO 休息区 6 个的限制
    // 卡牌 ID -> 卡牌实例列表
    private Map<Integer, List<CardInstance>> restZone = new HashMap<>();

    // 消耗牌堆
    private List<CardInstance> consumedDeck = new ArrayList<>();

}
