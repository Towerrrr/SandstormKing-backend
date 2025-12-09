package com.t0r.sandstormkingbackend.game.challenger.model.vo;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CupInstanceDeck;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.DrawSchedule;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class RoomGameStateVO {

    private Long roomId;

    private String version;

    private Integer totalPlayerCount;

    private Integer battlefieldCount;

    private boolean hasBot;

    // 回合数 -> 抽卡计划
    private Map<String, DrawSchedule> drawSchedules = new HashMap<>();

//    变化域

    private String currentRound;

    private Map<String, CupInstanceDeck> cupInstances = new ConcurrentHashMap<>();

    private Map<String, LinkedList<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private Map<String, LinkedList<CardInstance>> discardDecks = new ConcurrentHashMap<>();

}
