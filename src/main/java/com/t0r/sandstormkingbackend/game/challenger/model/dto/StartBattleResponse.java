package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battle;
import lombok.Data;

import java.util.LinkedList;

@Data
public class StartBattleResponse {

    String startPlayerId;

    String startWay;

    LinkedList<Battle> battleList;

    public StartBattleResponse(Long startPlayerId, String startWay, LinkedList<Battle> battleList) {
        this.startPlayerId = startPlayerId.toString();
        this.startWay = startWay;
        this.battleList = battleList;
    }

}
