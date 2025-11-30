package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChallengerPlayer {

    private Long userId;

    // 回合数 -> 战场名
    private Map<String, String> battlefieldSchedules = new HashMap<>();

    private List<CardInstance> cardInstances = new ArrayList<>();

    private List<CupInstance> cupInstances = new ArrayList<>();

    private Integer extraFanCount = 0;

    private Integer totalFanCount = 0;

}
