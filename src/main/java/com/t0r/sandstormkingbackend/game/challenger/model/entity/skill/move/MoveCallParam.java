package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;

import java.util.*;

@Data
public class MoveCallParam {

    // 牌堆等级 -> 卡牌实例 （主牌堆、弃牌堆）
    private Map<String, LinkedList<CardInstance>> mainDecks = null;
    private Map<String, LinkedList<CardInstance>> discardDecks = null;

    private LinkedList<CardInstance> handZone = null;
    private Map<String, LinkedList<CardInstance>> restZone = null;
    private List<CardInstance> consumedDeck = null;

}
