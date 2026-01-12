package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector;

import com.t0r.sandstormkingbackend.Util.SpringContextHolder;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleStateEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.OptionalStartEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.event.CardSelectEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;


import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

@Slf4j
@UtilityClass
public class CardSelector {

    public Mono<Void> apply(CardInstance cardInstance, BattleSeat attacker,
                            ApplicationEventPublisher eventPublisher, Battlefield battlefield) {
        String waitKey = "user_" + attacker.getUserId();

        PlayerWaitManager playerWaitManager = SpringContextHolder.getBean(PlayerWaitManager.class);
        Mono<CardSelectorResponse> mono = playerWaitManager.createWaitMono(waitKey, CardSelectorResponse.class)
                .doOnCancel(() -> log.info("Wait cancelled for {}", waitKey));

        Card card = cardMap.get(cardInstance.getCardId());
        eventPublisher.publishEvent( // 通知前端选牌
                new CardSelectEvent(attacker.getUserId(), buildRequest(card.getCardSelectorParam(), attacker))
        );

        return mono
                .doOnSuccess(cardSelectorResponse -> {
                    // TODO 根据选择卡响应做响应操作
                })
                .then()
                .doOnSuccess(v -> battlefield.setCurrentState(BattleStateEnum.applyAttackDamage));
    }

    private CardSelectorRequest buildRequest(CardSelectorParam cardSelectorParam,
                                             BattleSeat attacker) {
        CardSelectorRequest cardSelectorRequest = new CardSelectorRequest();
        OptionalStartEnum optionalStart = cardSelectorParam.getOptionalStart();
        if (optionalStart == null) {
            throw new RuntimeException("optionalStart is null");
        } else {
            switch (optionalStart) {
                case HAND_ZONE:
                    cardSelectorRequest.setHandZoneOrConsumedDeck(attacker.getHandZone());
                    break;
                case CONSUMED_DECK:
                    cardSelectorRequest.setHandZoneOrConsumedDeck(attacker.getConsumedDeck());
                    break;
                case REST_ZONE:
                    cardSelectorRequest.setRestZone(attacker.getRestZone());
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
