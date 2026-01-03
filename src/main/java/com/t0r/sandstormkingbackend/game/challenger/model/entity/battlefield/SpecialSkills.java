package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.SpecialCardsEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpecialSkills {

    public int calculateRealTimePower(Card card, int currentRound) {
        if (SpecialCardsEnum.MACHINE.getName().equals(card.getName())) {
            return currentRound;
        }
        return card.getBasePower();
    }

    public boolean checkInstantWin(Card card, BattleSeat attacker) {
        if (SpecialCardsEnum.ZEPPELIN.getName().equals(card.getName())) {
            return attacker.hasLevelCCardInRestZone();
        }
        return false;
    }

    public int getGainCoefficient(Card card) {
        if (SpecialCardsEnum.STREAMER.getName().equals(card.getName())) {
            return 2;
        }
        return 1;
    }
}