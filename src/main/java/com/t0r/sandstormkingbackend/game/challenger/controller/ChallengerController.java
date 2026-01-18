package com.t0r.sandstormkingbackend.game.challenger.controller;

import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.InitGameRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.StartBattleResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPutRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPutResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.ChallengerMessageTypeEnum;
import com.t0r.sandstormkingbackend.handler.RSocketGameHandler;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.enums.WSMessageTypeEnum;
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

    @Resource
    private PlayerWaitManager playerWaitManager;

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

    @MessageMapping("challenger.cardSelect")
    public Mono<Void> onPlayerSelectCard(CardSelectorResponse cardSelectorResponse) {
        String waitKey = "user_" + cardSelectorResponse.getUserId();
        playerWaitManager.completeWaitMono(waitKey, cardSelectorResponse);
        return Mono.empty();
    }

    @MessageMapping("challenger.checkAndPut")
    public Mono<Void> onPlayerCheckAndPut(CheckAndPutResponse checkAndPutResponse) {
        String waitKey = "user_" + checkAndPutResponse.getUserId();
        playerWaitManager.completeWaitMono(waitKey, checkAndPutResponse);
        return Mono.empty();
    }

    public void notifyPlayerWaitOpponent(Long userId, Long opponentId) {
        WSMessage waitMsgToUser = new WSMessage(
                WSMessageTypeEnum.CHALLENGER.getValue(),
                null,
                new GameMessage(
                        ChallengerMessageTypeEnum.WAIT_OPPONENT_READY.getValue(),
                        null, null, null)
        );
        WSMessage waitMsgToOpponent = new WSMessage(
                WSMessageTypeEnum.CHALLENGER.getValue(),
                null,
                new GameMessage(
                        ChallengerMessageTypeEnum.WAIT_YOU_READY.getValue(),
                        null, null, null)
        );
        rSocketGameHandler.sendToUser(userId, waitMsgToUser);
        rSocketGameHandler.sendToUser(opponentId, waitMsgToOpponent);
    }

    public void notifyPlayerStartBattle(Long userId, Long opponentId, StartBattleResponse startBattleResponse) {
        WSMessage startMsgToUser = new WSMessage(
                WSMessageTypeEnum.CHALLENGER.getValue(),
                null,
                new GameMessage(
                        ChallengerMessageTypeEnum.START_BATTLE.getValue(),
                        null, null, JSONUtil.toJsonStr(startBattleResponse))
        );
        rSocketGameHandler.sendToUser(userId, startMsgToUser);
        rSocketGameHandler.sendToUser(opponentId, startMsgToUser);
    }

    public void notifyPlayerCardSelect(Long userId, CardSelectorRequest cardSelectorRequest) {
        WSMessage cardSelectMsgToUser = new WSMessage(
                WSMessageTypeEnum.CHALLENGER.getValue(),
                null,
                new GameMessage(
                        ChallengerMessageTypeEnum.SELECT_CARD.getValue(),
                        null, null, JSONUtil.toJsonStr(cardSelectorRequest))
        );
        rSocketGameHandler.sendToUser(userId, cardSelectMsgToUser);
    }

    public void notifyPlayerCheckAndPut(long userId, CheckAndPutRequest checkAndPutRequest) {
        WSMessage checkAndPutMsgToUser = new WSMessage(
                WSMessageTypeEnum.CHALLENGER.getValue(),
                null,
                new GameMessage(
                        ChallengerMessageTypeEnum.CHECK_AND_PUT.getValue(),
                        null, null, JSONUtil.toJsonStr(checkAndPutRequest))
        );
        rSocketGameHandler.sendToUser(userId, checkAndPutMsgToUser);
    }
}
