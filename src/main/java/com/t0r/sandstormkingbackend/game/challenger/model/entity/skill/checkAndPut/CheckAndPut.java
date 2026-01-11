package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;

import com.t0r.sandstormkingbackend.Util.SpringContextHolder;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleStateEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TimeRangeEnum;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.LinkedList;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

/**
 * 一个位置放置多个卡时，顺序由前端指定
 */
@Slf4j
@UtilityClass
public class CheckAndPut {

    // TODO 后续重构 Battlefield
    public Mono<Void> apply(CardInstance cardInstance, BattleSeat battleSeat,
                            ApplicationEventPublisher eventPublisher, Battlefield battlefield) {
        String waitKey = "user_" + battleSeat.getUserId();
        PlayerWaitManager playerWaitManager = SpringContextHolder.getBean(PlayerWaitManager.class);
        Mono<CheckAndPutResponse> mono = playerWaitManager.createWaitMono(waitKey, CheckAndPutResponse.class)
                .doOnCancel(() -> log.info("Wait cancelled for {}", waitKey));

        Card card = cardMap.get(cardInstance.getCardId());
        CheckAndPutParam checkAndPutParam = card.getCheckAndPutParam();
        if (checkAndPutParam != null) {
            WhereToCheckEnum whereToCheckEnum = checkAndPutParam.getWhereToCheckEnum();
            Integer count = checkAndPutParam.getCount();
            WhereToPutEnum[] whereToPutEnums = checkAndPutParam.getWhereToPutEnums();

            switch (whereToCheckEnum) {
                case HAND_ZONE_TOP:
                    if (count != null) {
                        eventPublisher.publishEvent(new CheckAndPutEvent(
                                battleSeat.getUserId(),
                                new CheckAndPutRequest(battleSeat.popTopHandZone(count), whereToPutEnums)));
                    } else {
                        log.error("CheckAndPut(skill): HAND_ZONE_TOP(WhereToCheck), count is null");
                    }
                    break;
                case HAND_ZONE_BOTTOM:
                    if (count != null) {
                        eventPublisher.publishEvent(new CheckAndPutEvent(
                                battleSeat.getUserId(),
                                new CheckAndPutRequest(battleSeat.popBottomHandZone(count), whereToPutEnums)));
                    } else {
                        log.error("CheckAndPut(skill): HAND_ZONE_BOTTOM(WhereToCheck), count is null");
                    }
                    break;
                case HAND_ZONE_ALL:
                    eventPublisher.publishEvent(new CheckAndPutEvent(
                            battleSeat.getUserId(),
                            new CheckAndPutRequest(battleSeat.getHandZone(), whereToPutEnums)));
                    break;
                default:
                    throw new RuntimeException("whereToCheckEnum is not valid");
            }
            return mono
                    .doOnSuccess(response -> {
                        judgeHandZoneAll(whereToCheckEnum, battleSeat.getHandZone(), response.getCardInstanceId());
                        putCard(response, battleSeat);
                    })
                    .then()
                    .doOnSuccess(v -> {
                        if (card.getTimeRange().equals(TimeRangeEnum.LOSE_FLAG.getValue())) {
                            battlefield.setCurrentState(BattleStateEnum.moveAttackerToRestZone);
                        } else {
                            battlefield.setCurrentState(BattleStateEnum.triggerAttackerBuffs);
                        }
                    });
        } else {
            return Mono.empty();
        }
    }

    private void judgeHandZoneAll(WhereToCheckEnum whereToCheckEnum,
                                  LinkedList<CardInstance> handZone, Integer cardInstanceId) {
        if (whereToCheckEnum == WhereToCheckEnum.HAND_ZONE_ALL) {
            handZone.stream()
                    .filter(cardInstance -> cardInstance.getId().equals(cardInstanceId))
                    .findFirst()
                    .ifPresent(handZone::remove);
        }
    }

    private void putCard(CheckAndPutResponse checkAndPutResponse, BattleSeat battleSeat) {
        Arrays.stream(checkAndPutResponse.getPuts()).forEach(
                put -> {
                    LinkedList<CardInstance> cardInstances = put.getCardInstances();
                    if (cardInstances != null) {
                        switch (put.getWhereToPutEnum()) {
                            case HAND_ZONE_TOP:
                                battleSeat.addCardsToHandZoneHead(cardInstances);
                                break;
                            case HAND_ZONE_BOTTOM:
                                battleSeat.addCardsToHandZoneTail(cardInstances);
                                break;
                            case CONSUMED_DECK:
                                battleSeat.addCardsToConsumedDeck(cardInstances);
                                break;
                            default:
                                throw new RuntimeException("whereToPutEnum is not valid");
                        }
                    }
                }
        );
    }

}
