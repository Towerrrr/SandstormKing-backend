package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.OptionalStartEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.StartEnum;
import lombok.Data;

@Data
public class CardSelectorParam {

    private OptionalStartEnum optionalStart;

    private Integer count;
    private Integer maxCount;

    private CardFilter cardFilter;

}
