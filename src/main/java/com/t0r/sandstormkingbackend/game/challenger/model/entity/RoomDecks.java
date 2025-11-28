package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class RoomDecks {

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

    // 用于从战场角度记录对战计划
    // 回合数 -> 战场 -> 2 个用户 ID
    Map<String, Map<String, List<Long>>> battlefieldSchedules = new HashMap<>();

    private String currentRound;

    // 回合数 -> 奖杯实例
    private Map<String, CupInstanceDeck> cupInstances = new ConcurrentHashMap<>();

    // 牌堆等级 -> 卡牌实例 （主牌堆、弃牌堆）
    private Map<String, List<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private Map<String, List<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // 用于临时战斗
    // 战场名 -> 玩家 ID -> 半场
    private Map<String, Map<Long, HalfBattlefield>> battlefields = new ConcurrentHashMap<>();

    public List<CardInstance> getMainDeck(String key) {
        return mainDecks.get(key);
    }

    public void addMainDeck(String key, List<CardInstance> deck) {
        mainDecks.put(key, deck);
    }

    public void addDiscardDeck(String key, List<CardInstance> deck) {
        discardDecks.put(key, deck);
    }
}

