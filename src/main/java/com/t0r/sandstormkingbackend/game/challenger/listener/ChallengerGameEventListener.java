package com.t0r.sandstormkingbackend.game.challenger.listener;

import com.t0r.sandstormkingbackend.game.challenger.controller.ChallengerController;
import com.t0r.sandstormkingbackend.game.challenger.model.event.PlayerReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ChallengerGameEventListener {

    private final ChallengerController challengerController;

    public ChallengerGameEventListener(ChallengerController challengerController) {
        this.challengerController = challengerController;
    }

    @EventListener
    public void onPlayerReady(PlayerReadyEvent event) {
        // 调用控制器接口，向用户和对手推送“等待对手/等待你准备”消息
        challengerController.notifyPlayerWaitOpponent(
                event.getUserId(), event.getOpponentId(), event.getBattlefield()
        );
    }

}
