package com.t0r.sandstormkingbackend.game.challenger.model.event;

import com.t0r.sandstormkingbackend.game.challenger.model.dto.StartBattleResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StartBattleEvent {

    private final Long userId;

    private final Long opponentId;

    StartBattleResponse startBattleResponse;

}
