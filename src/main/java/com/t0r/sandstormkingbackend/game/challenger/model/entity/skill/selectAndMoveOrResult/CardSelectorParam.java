package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.OptionalStartEnum;
import lombok.Data;

@Data
public class CardSelectorParam {

    private SelectTargetEnum selectTargetEnum = SelectTargetEnum.SELF;

    private OptionalStartEnum optionalStart;

    private Integer count;
    private Integer maxCount;

    private CardFilter cardFilter;

}
