package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import com.t0r.sandstormkingbackend.Util.SpringContextHolder;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleStateEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Power;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult.ConditionAndResultParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult.ResultEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.Move;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveConfigParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveTargetEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.OptionalStartEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TimeRangeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.event.CardSelectEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

// TODO 修改类名
@Slf4j
@UtilityClass
public class CardSelector {

    public Mono<Void> apply(CardInstance cardInstance, BattleSeat self, BattleSeat opponent,
                            ApplicationEventPublisher eventPublisher,
                            Battlefield battlefield, Power tempAttackerPower) {
        Card card = cardMap.get(cardInstance.getCardId());
        CardSelectorParam cardSelectorParam = card.getCardSelectorParam();
        BattleSeat selectTarget = cardSelectorParam.getSelectTargetEnum().equals(SelectTargetEnum.SELF) ? self : opponent;
        String waitKey = "user_" + selectTarget.getUserId();

        PlayerWaitManager playerWaitManager = SpringContextHolder.getBean(PlayerWaitManager.class);
        Mono<CardSelectorResponse> mono = playerWaitManager.createWaitMono(waitKey, CardSelectorResponse.class)
                .doOnCancel(() -> log.info("Wait cancelled for {}", waitKey));

        MoveTargetEnum moveTargetEnum = card.getMoveTargetEnum();
        BattleSeat moveTarget = MoveTargetEnum.SELF.equals(card.getMoveTargetEnum()) ? self : opponent;
        CardSelectorRequest cardSelectorRequest = buildRequest(card.getTimeRange(), cardSelectorParam, moveTarget);
        eventPublisher.publishEvent( // 通知前端选牌
                new CardSelectEvent(selectTarget.getUserId(), cardSelectorRequest)
        );

        return mono
                .doOnSuccess(cardSelectorResponse -> {
                    moveOrResult(card, cardSelectorResponse, moveTarget, tempAttackerPower);
                })
                .then()
                .doOnSuccess(v -> battlefield.setCurrentState(BattleStateEnum.triggerAttackerBuffs));
    }

    private void moveOrResult(Card card, CardSelectorResponse cardSelectorResponse, BattleSeat self, Power tempAttackerPower) {
        String userId = cardSelectorResponse.getUserId();
        if (cardSelectorResponse.getIsTrigger().equals(Boolean.FALSE)) {
            log.info("用户 {} 选择不触发卡牌效果", userId);
            return;
        }

        CardSelectorParam cardSelectorParam = card.getCardSelectorParam();
        MoveConfigParam moveConfigParam = card.getMoveConfigParam();
        if (moveConfigParam != null) {
            Move move = new Move(moveConfigParam);
            Integer selectedCardInstanceId = cardSelectorResponse.getSelectedCardInstanceId();
            if (selectedCardInstanceId != null) {
                CardInstance cardInstance = removeCardInstance(self, cardSelectorParam.getOptionalStart(), selectedCardInstanceId);
                move.apply(self, cardInstance);
            }
            Set<Integer> selectedCardInstanceIds = cardSelectorResponse.getSelectedCardInstanceIds();
            if (selectedCardInstanceIds != null && !selectedCardInstanceIds.isEmpty()) {
                LinkedList<CardInstance> cardInstances = removeCardInstances(self, cardSelectorParam.getOptionalStart(), selectedCardInstanceIds);

                move.apply(self, cardInstances);
            }
        }

        ConditionAndResultParam conditionAndResultParam = card.getConditionAndResultParam();
        if (conditionAndResultParam != null) {
            ResultEnum resultEnum = conditionAndResultParam.getResultEnum();
            int resultIncrement = conditionAndResultParam.getResultIncrement();
            if (ResultEnum.THIS_CARD_POWER.equals(resultEnum)) {
                tempAttackerPower.addBase(resultIncrement);
            }
        }

    }

    private CardSelectorRequest buildRequest(String timeRangeEnum,
                                             CardSelectorParam cardSelectorParam,
                                             BattleSeat seat) {
        CardSelectorRequest cardSelectorRequest = new CardSelectorRequest();

        if (timeRangeEnum.equals(TimeRangeEnum.OPTIONAL.getValue())) {
            cardSelectorRequest.setIsOptional(true);
        }

        OptionalStartEnum optionalStart = cardSelectorParam.getOptionalStart();
        if (optionalStart == null) {
            throw new RuntimeException("optionalStart is null");
        } else {
            switch (optionalStart) {
                case HAND_ZONE:
                    cardSelectorRequest.setCandidateCards(seat.getHandZone());
                    break;
                case CONSUMED_DECK:
                    cardSelectorRequest.setCandidateCards(seat.getConsumedDeck());
                    break;
                case REST_ZONE:
                    cardSelectorRequest.setCandidateCards(seat.getAllCardsInRestZone());
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

    private CardInstance removeCardInstance(BattleSeat seat, OptionalStartEnum zone, Integer instanceId) {
        CardInstance targetCard = null;

        switch (zone) {
            case HAND_ZONE:
                targetCard = findAndRemoveFromList(seat.getHandZone(), instanceId);
                break;
            case CONSUMED_DECK:
                targetCard = findAndRemoveFromList(seat.getConsumedDeck(), instanceId);
                break;
            case REST_ZONE:
                targetCard = seat.removeCardFromRestZone(instanceId);
                break;
            default:
                throw new IllegalArgumentException("Unknown zone: " + zone);
        }

        if (targetCard == null) {
            throw new IllegalArgumentException("Card " + instanceId + " not found in " + zone);
        }

        return targetCard;
    }

    private LinkedList<CardInstance> removeCardInstances(BattleSeat seat, OptionalStartEnum zone, Set<Integer> instanceIds) {
        LinkedList<CardInstance> resultList = new LinkedList<>();
        if (instanceIds == null || instanceIds.isEmpty()) {
            return resultList;
        }

        for (Integer id : instanceIds) {
            CardInstance card = removeCardInstance(seat, zone, id);
            resultList.add(card);
        }
        return resultList;
    }

    private CardInstance findAndRemoveFromList(List<CardInstance> list, Integer instanceId) {
        Iterator<CardInstance> it = list.iterator();
        while (it.hasNext()) {
            CardInstance card = it.next();
            if (card.getId().equals(instanceId)) {
                it.remove();
                return card;
            }
        }
        return null;
    }

}
