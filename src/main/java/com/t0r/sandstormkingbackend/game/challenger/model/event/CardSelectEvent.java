package com.t0r.sandstormkingbackend.game.challenger.model.event;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CardSelectEvent {

    private final Long userId;

    CardSelectorRequest cardSelectorRequest;

}
