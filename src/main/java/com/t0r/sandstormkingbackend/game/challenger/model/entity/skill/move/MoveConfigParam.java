package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import lombok.Data;

@Data
public class MoveConfigParam {

    private String permission = PermissionEnum.MUST.getValue();

    private String target = TargetEnum.SELF.getValue();

    private String optionalStart;
    private String start;

    private Integer count;
    private Integer maxCount;

    private String type = MoveTypeEnum.CARD_INSTANCE.getValue();

    private CardFilter cardFilter = null;

    private String end;

}
