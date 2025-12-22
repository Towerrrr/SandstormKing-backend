package com.t0r.sandstormkingbackend.game.challenger.controller;

import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.InitGameRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.handler.RSocketGameHandler;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.enums.MessageBroadcastTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Map;

@Controller
@Slf4j
public class ChallengerController {

    @Resource
    private RSocketGameHandler rSocketGameHandler;

    @Resource
    private ChallengerGameManager challengerGameManager;

    @MessageMapping("challenger.initGame")
    public Mono<Map<Integer, Card>> initGame(@Payload InitGameRequest initGameRequest) {
        log.info("挑战者初始化: {}", initGameRequest);
        try {
            Map<Integer, Card> cardMap = challengerGameManager.initGame(initGameRequest);
            return Mono.just(cardMap);
        } catch (Exception e) {
            log.error("initGame error", e);
            return Mono.error(e);
        }
    }

    // TODO 供事件监听器调用
    public void notifyPlayerWaitOpponent(Long userId, Long opponentId, String battlefield) {
        WSMessage waitMsgToUser = new WSMessage(
                // 填写消息类型、内容
        );
        WSMessage waitMsgToOpponent = new WSMessage(
                // 填写消息类型、内容
        );
        // 推送消息到 userId 和 opponentId 的 websocket/rSocket session
        rSocketGameHandler.sendToUser(userId, waitMsgToUser);
        rSocketGameHandler.sendToUser(opponentId, waitMsgToOpponent);
    }

}
