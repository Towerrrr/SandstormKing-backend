package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.SpecialCardsEnum;
import lombok.experimental.UtilityClass;

/**
 * 工具类：有涉及特殊卡技能的逻辑，也有普通卡相关的实现。
 */
@UtilityClass
public class SpecialSkills {

    public int calculateRealTimePower(Card card, String currentRound) {
        if (SpecialCardsEnum.MACHINE.getName().equals(card.getName())) {
            return Integer.parseInt(currentRound);
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