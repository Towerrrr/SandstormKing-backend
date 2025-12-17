package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Power;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BuffCallParam {

    private String currentTimeRange;

    private Power currentPower;

    private Card card;
}
