package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import lombok.Data;

import java.util.LinkedList;
import java.util.Map;

/**
 * 服务器对前端的请求
 */
@Data
public class CardSelectorRequest {

    private Map<String, LinkedList<CardInstance>> restZone;

    private LinkedList<CardInstance> handZoneOrConsumedDeck;

    private Integer count;
    private Integer maxCount;

    private CardFilter cardFilter;

}
