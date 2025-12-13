package com.t0r.sandstormkingbackend.game.challenger.model.vo;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CupInstance;
import lombok.Data;

import java.util.*;

@Data
public class ChallengerPlayerVO {

    private String userId;

    // 回合数 -> 战场名
    private Map<String, String> battlefieldSchedules;

    private List<Integer> cupInstanceRounds;

    private Integer extraFanCount;

    private Integer totalFanCount;

    public ChallengerPlayerVO(ChallengerPlayer challengerPlayer) {
        this.userId = challengerPlayer.getUserId().toString();
        this.battlefieldSchedules = challengerPlayer.getBattlefieldSchedules();
        challengerPlayer.getCupInstances().forEach(
                cupInstance -> this.cupInstanceRounds.add(Integer.valueOf(cupInstance.getRound())));
        this.extraFanCount = challengerPlayer.getExtraFanCount();
        this.totalFanCount = challengerPlayer.getTotalFanCount();
    }
}
