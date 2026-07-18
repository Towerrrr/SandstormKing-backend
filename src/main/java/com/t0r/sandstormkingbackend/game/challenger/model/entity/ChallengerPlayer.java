package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import com.t0r.sandstormkingbackend.game.challenger.model.enums.RoundEnum;
import lombok.Data;

import java.util.*;

@Data
public class ChallengerPlayer {

    private Long userId;

    // 回合数 -> 战场名
    private Map<String, String> battlefieldSchedules;
    private Map<String, Boolean> battlefieldResults = new HashMap<>();

    private LinkedList<CardInstance> handCardInstances = new LinkedList<>();

    private int drawAttemptCount = 0; // 已抽取次数
    private int drawCardCount = 0; // 已抽取卡牌数
    private LinkedList<CardInstance> tempSelectedCardInstances = new LinkedList<>();

    private List<CupInstance> cupInstances = new ArrayList<>();

    private Integer extraFanCount = 0;

    private Integer totalFanCount = 0;

    public ChallengerPlayer(Long userId, Map<String, String> battlefieldSchedules) {
        this.userId = userId;
        this.battlefieldSchedules = battlefieldSchedules;
        Arrays.stream(RoundEnum.values()).forEach(roundEnum -> this.battlefieldResults.put(roundEnum.getValue(), null));
    }

    public void addExtraFanCount(int value) {
        this.extraFanCount += value;
    }
    public void addDrawAttemptCount(int value) {
        this.drawAttemptCount += value;
    }
    public void addDrawCardCount(int value) {
        this.drawCardCount += value;
    }

    public boolean isPreviousRoundLose(String currentRound) {
        RoundEnum current = RoundEnum.getByValue(currentRound);
        RoundEnum previous = null;
        if (current != null) {
            previous = current.getPreviousRound();
        }
        Boolean result = null;
        if (previous != null) {
            result = this.battlefieldResults.get(previous.getValue());
        }

        return Boolean.FALSE.equals(result);
    }

}
