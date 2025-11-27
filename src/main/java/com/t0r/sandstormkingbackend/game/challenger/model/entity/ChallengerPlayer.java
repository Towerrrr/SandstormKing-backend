package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChallengerPlayer {

    private Long userId;

    // 回合数 -> 战场名
    private Map<Integer, String> battlefieldSchedules;

    private List<CardInstance> cardInstances;

    private Integer cupCount;

    private Integer extraFanCount;

    private Integer totalFanCount;

}
