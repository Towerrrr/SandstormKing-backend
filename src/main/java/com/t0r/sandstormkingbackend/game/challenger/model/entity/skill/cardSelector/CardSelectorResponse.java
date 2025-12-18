package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import lombok.Data;

import java.util.Set;

/**
 * 前端对服务器的相应
 */
@Data
public class CardSelectorResponse {

    private Set<Integer> selectedCardInstanceIds;

}
