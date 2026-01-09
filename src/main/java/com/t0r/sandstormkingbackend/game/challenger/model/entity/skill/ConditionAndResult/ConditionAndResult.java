package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Power;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConditionAndResult {

    public void apply(Card card, BattleSeat self, BattleSeat opponent,
                      ChallengerPlayer selfInfo, ChallengerPlayer opponentInfo, Power tempAttackerPower) {
        ConditionAndResultParam conditionAndResultParam = card.getConditionAndResultParam();
        ConditionEnum conditionEnum = conditionAndResultParam.getConditionEnum();
        ResultEnum resultEnum = conditionAndResultParam.getResultEnum();
        int resultIncrement = conditionAndResultParam.getResultIncrement();

        boolean condition = false;
        int conditionValue = 0;
        switch (conditionEnum) {
            case PREVIOUS_MATCH_LOST:
                // TODO 上一场输了
                break;
            case HAS_CARD_UNDERNEATH:
                condition = self.hasCardInHandZone();
                break;
            case HAND_NEARLY_EMPTY:
                condition = self.getHandZoneSize() <= 1;
                break;
            case OPPONENT_REST_HAS_ROOKIE:
                condition = opponent.hasRookieInRestZone();
                break;
            case OPPONENT_CONSUMED_NOT_EMPTY:
                condition = opponent.hasCardInConsumedDeck();
                break;
            // PER_，每...
            case PER_CONSUMED_CARD:
                conditionValue = self.getConsumedDeckSize();
                break;
            case OPPONENT_PER_CONSUMED_CARD:
                conditionValue = opponent.getConsumedDeckSize();
                break;
            case OPPONENT_PER_HAS_CUP:
                conditionValue = opponentInfo.getCupInstances().size();
                break;
            default:
                throw new RuntimeException("ConditionAndResult.apply: Unknown condition");
        }

        switch (resultEnum) {
            case THIS_CARD_POWER:
                // TODO
                break;
            case FAN_COUNT:
                // TODO
                break;
            default:
                throw new RuntimeException("ConditionAndResult.apply: Unknown result");
        }

    }
}
