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

    /**
     * 默认必选，部分卡可不选。
     */
    private Boolean isOptional = false;

    private Map<String, LinkedList<CardInstance>> restZone = null;

    private LinkedList<CardInstance> handZoneOrConsumedDeck = null;

    private Integer count;
    private Integer maxCount;

    private CardFilter cardFilter;

}
