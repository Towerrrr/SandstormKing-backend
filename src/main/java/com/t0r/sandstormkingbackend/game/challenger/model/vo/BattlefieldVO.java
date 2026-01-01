package com.t0r.sandstormkingbackend.game.challenger.model.vo;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battle;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import lombok.Data;

import java.util.LinkedList;
import java.util.Map;

@Data
public class BattlefieldVO {

    String name;

    String currentPhase;

    Map<Long, BattleSeat> halfBattlefieldMap;

    String startPlayerId;
    String elsePlayerId;

    LinkedList<Battle> battleList;

    boolean isEnd;
    String winnerId;

    public BattlefieldVO(Battlefield battlefield) {
        this.name = battlefield.getName();
        this.currentPhase = battlefield.getCurrentPhase();
        this.halfBattlefieldMap = battlefield.getHalfBattlefieldMap();
        this.startPlayerId = String.valueOf(battlefield.getStartPlayerId());
        this.elsePlayerId = String.valueOf(battlefield.getElsePlayerId());
        this.battleList = battlefield.getBattleList();
        this.isEnd = battlefield.isEnd();
        this.winnerId = String.valueOf(battlefield.getWinnerId());
    }
}
