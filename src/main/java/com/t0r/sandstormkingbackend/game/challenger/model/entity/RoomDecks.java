package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class RoomDecks {

    private Map<String, List<CardInstance>> mainDecks = new ConcurrentHashMap<>();

    private Map<String, List<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

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

