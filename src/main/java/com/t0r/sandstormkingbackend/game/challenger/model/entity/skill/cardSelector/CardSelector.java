package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.OptionalStartEnum;

import java.util.LinkedList;
import java.util.Map;

public class CardSelector {

    public static CardSelectorRequest buildRequest(CardSelectorParam cardSelectorParam,
                                                   LinkedList<CardInstance> handZone,
                                                   Map<String, LinkedList<CardInstance>> restZone,
                                                   LinkedList<CardInstance> consumedDeck) {
        CardSelectorRequest cardSelectorRequest = new CardSelectorRequest();
        OptionalStartEnum optionalStart = cardSelectorParam.getOptionalStart();
        if (optionalStart == null) {
            throw new RuntimeException("optionalStart is null");
        } else {
            switch (optionalStart) {
                case HAND_ZONE:
                    cardSelectorRequest.setHandZoneOrConsumedDeck(handZone);
                    break;
                case CONSUMED_DECK:
                    cardSelectorRequest.setHandZoneOrConsumedDeck(consumedDeck);
                    break;
                case REST_ZONE:
                    cardSelectorRequest.setRestZone(restZone);
                    break;
                default:
                    throw new RuntimeException("optionalStart error");
            }
        }

        cardSelectorRequest.setCount(cardSelectorParam.getCount());
        cardSelectorRequest.setMaxCount(cardSelectorParam.getMaxCount());
        cardSelectorRequest.setCardFilter(cardSelectorParam.getCardFilter());
        return cardSelectorRequest;
    }

}
