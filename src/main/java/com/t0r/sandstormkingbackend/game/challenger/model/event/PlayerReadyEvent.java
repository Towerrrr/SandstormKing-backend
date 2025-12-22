package com.t0r.sandstormkingbackend.game.challenger.model.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerReadyEvent {

    private final Long roomId;

    private final Long userId;

    private final Long opponentId;

    private final String battlefield;

}
