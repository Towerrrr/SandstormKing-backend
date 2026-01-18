package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import lombok.Data;

import java.util.List;

/**
 * 服务器对前端的请求
 */
@Data
public class CardSelectorRequest {

    /**
     * 默认必选，部分卡可不选。
     */
    private Boolean isOptional = false;

    private List<CardInstance> candidateCards;

    private Integer count;
    private Integer maxCount;

    private CardFilter cardFilter;

}
