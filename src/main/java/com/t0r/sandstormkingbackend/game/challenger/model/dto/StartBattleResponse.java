package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battle;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleLog;
import lombok.Data;

import java.util.LinkedList;

@Data
public class StartBattleResponse {

    String startPlayerId;

    String startWay;

    BattleLog battleLog;

    public StartBattleResponse(Long startPlayerId, String startWay, BattleLog battleLog) {
        this.startPlayerId = startPlayerId.toString();
        this.startWay = startWay;
        this.battleLog = battleLog;
    }

}
