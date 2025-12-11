package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedList;

@Data
@AllArgsConstructor
public class StartBattleResponse {

    Long startPlayerId;

    String startWay;

    LinkedList<Battle> battleList;

}
