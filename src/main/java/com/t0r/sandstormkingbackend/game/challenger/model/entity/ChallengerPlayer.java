package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class ChallengerPlayer {

    private Long userId;

    private List<Integer> cardIds;

    private Integer cupCount;

    private Integer extraFanCount;

    private Integer totalFanCount;

}
