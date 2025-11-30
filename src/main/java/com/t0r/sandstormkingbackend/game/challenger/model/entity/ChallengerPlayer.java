package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.*;

@Data
public class ChallengerPlayer {

    private Long userId;

    // 回合数 -> 战场名
    private Map<String, String> battlefieldSchedules = new HashMap<>();

    private LinkedList<CardInstance> handCardInstances = new LinkedList<>();

    private boolean isSecondSelect = true;
    private LinkedList<CardInstance> tempSelectedCardInstances = new LinkedList<>();

    private List<CupInstance> cupInstances = new ArrayList<>();

    private Integer extraFanCount = 0;

    private Integer totalFanCount = 0;

}
