package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckAndPutEvent {

    private final Long userId;

    private final CheckAndPutRequest checkAndPutRequest;

}
