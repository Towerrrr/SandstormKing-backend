package com.t0r.sandstormkingbackend.game.challenger.model.vo;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CupInstance;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class ChallengerPlayerSelf {

    private String userId;

    // 回合数 -> 战场名
    private Map<String, String> battlefieldSchedules;

    private List<CupInstance> cupInstances;

    private Integer extraFanCount;

    private Integer totalFanCount;

    private LinkedList<CardInstance> handCardInstances;

    public ChallengerPlayerSelf(ChallengerPlayer challengerPlayer) {
        this.userId = challengerPlayer.getUserId().toString();
        this.battlefieldSchedules = challengerPlayer.getBattlefieldSchedules();
        this.cupInstances = challengerPlayer.getCupInstances();
        this.extraFanCount = challengerPlayer.getExtraFanCount();
        this.totalFanCount = challengerPlayer.getTotalFanCount();
        this.handCardInstances = challengerPlayer.getHandCardInstances();
    }
}
