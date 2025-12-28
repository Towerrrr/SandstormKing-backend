package com.t0r.sandstormkingbackend.game.challenger.listener;

import com.t0r.sandstormkingbackend.game.challenger.controller.ChallengerController;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager;
import com.t0r.sandstormkingbackend.game.challenger.model.event.CardSelectEvent;
import com.t0r.sandstormkingbackend.game.challenger.model.event.EndBattleEvent;
import com.t0r.sandstormkingbackend.game.challenger.model.event.PlayerReadyEvent;
import com.t0r.sandstormkingbackend.game.challenger.model.event.StartBattleEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ChallengerGameEventListener {

    @Resource
    private ChallengerController challengerController;

    @Resource
    private ChallengerGameManager challengerGameManager;

    @EventListener
    public void onPlayerReady(PlayerReadyEvent event) {
        challengerController.notifyPlayerWaitOpponent(
                event.getUserId(), event.getOpponentId()
        );
    }

    @EventListener
    public void onStartBattle(StartBattleEvent event) {
        challengerController.notifyPlayerStartBattle(
                event.getUserId(), event.getOpponentId(), event.getStartBattleResponse()
        );
    }

    @EventListener
    public void onCardSelect(CardSelectEvent event) {
        challengerController.notifyPlayerCardSelect(event.getUserId(), event.getCardSelectorRequest());
    }

    @EventListener
    public void onEndBattle(EndBattleEvent event) {
        challengerGameManager.getRoomGameStateMap().get(event.getRoomId())
                .award(event.getBattlefield(), event.getWinnerId());
    }

}
