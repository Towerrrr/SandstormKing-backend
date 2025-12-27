package com.t0r.sandstormkingbackend.game.challenger.model.event;

import com.t0r.sandstormkingbackend.game.challenger.model.dto.StartBattleResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EndBattleEvent {

    private final Long roomId;

    private final String battlefield;

    private final Long winnerId;

}
