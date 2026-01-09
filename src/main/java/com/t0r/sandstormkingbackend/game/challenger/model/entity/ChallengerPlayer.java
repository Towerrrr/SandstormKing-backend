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

    private boolean isSecondSelect = false;
    private Set<CardInstance> selectedCards = new HashSet<>(); // 用来处理第一次选择只部分选择
    private LinkedList<CardInstance> tempSelectedCardInstances = new LinkedList<>();

    private List<CupInstance> cupInstances = new ArrayList<>();

    private Integer extraFanCount = 0;

    private Integer totalFanCount = 0;

    public ChallengerPlayer(Long userId, Map<String, String> battlefieldSchedules) {
        this.userId = userId;
        this.battlefieldSchedules = battlefieldSchedules;
        Arrays.stream(RoundEnum.values()).forEach(roundEnum -> this.battlefieldResults.put(roundEnum.getValue(), null));
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
