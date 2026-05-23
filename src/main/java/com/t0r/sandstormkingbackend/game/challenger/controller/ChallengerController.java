package com.t0r.sandstormkingbackend.game.challenger.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.*;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.RoomGameState;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPutRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPutResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.CardSelectorResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.ChallengerMessageTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.BattlefieldVO;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.ChallengerPlayerSelf;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.ChallengerPlayerVO;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.RoomGameStateVO;
import com.t0r.sandstormkingbackend.handler.RSocketGameHandler;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@Slf4j
public class ChallengerController {

    @Resource
    private RSocketGameHandler rSocketGameHandler;

    @Resource
    private ChallengerGameManager challengerGameManager;

    @Resource
    private PlayerWaitManager playerWaitManager;

    // ==================== Request-Response 端点 ====================

    @MessageMapping("challenger.initGame")
    public Mono<Map<Integer, Card>> initGame(@Payload InitGameRequest initGameRequest) {
        log.info("初始化游戏，房间：{}", initGameRequest.getRoomId());
        Map<Integer, Card> cardMap = challengerGameManager.initGame(initGameRequest);
        return Mono.just(cardMap);
    }

    @MessageMapping("challenger.getPlayer")
    public Mono<ChallengerPlayerSelf> getPlayer(@Payload GetPlayerRequest request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();
        log.info("获取玩家信息，房间：{}，用户：{}", roomId, userId);
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        ChallengerPlayer challengerPlayer = roomGameState.getChallengerPlayers().get(userId);
        ThrowUtils.throwIf(challengerPlayer == null, ErrorCode.NOT_FOUND_ERROR, "玩家不存在");
        
        return Mono.just(new ChallengerPlayerSelf(challengerPlayer));
    }

    @MessageMapping("challenger.getPlayerVO")
    public Mono<ChallengerPlayerVO> getPlayerVO(@Payload GetPlayerVORequest request) {
        Long roomId = request.getRoomId();
        Long targetUserId = request.getTargetUserId();
        log.info("获取玩家VO，房间：{}，目标用户：{}", roomId, targetUserId);
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        ChallengerPlayer challengerPlayer = roomGameState.getChallengerPlayers().get(targetUserId);
        ThrowUtils.throwIf(challengerPlayer == null, ErrorCode.NOT_FOUND_ERROR, "玩家不存在");
        
        return Mono.just(new ChallengerPlayerVO(challengerPlayer));
    }

    @MessageMapping("challenger.getBattlefield")
    public Mono<BattlefieldVO> getBattlefield(@Payload GetBattlefieldRequest request) {
        Long roomId = request.getRoomId();
        String battlefieldName = request.getBattlefieldName();
        log.info("获取战场信息，房间：{}，战场：{}", roomId, battlefieldName);
        
        ThrowUtils.throwIf(battlefieldName == null, ErrorCode.PARAMS_ERROR, "请选择战斗场");
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        Battlefield battlefield = roomGameState.getTempBattlefields().get(battlefieldName);
        ThrowUtils.throwIf(battlefield == null, ErrorCode.NOT_FOUND_ERROR, "战场不存在");
        
        return Mono.just(new BattlefieldVO(battlefield));
    }

    @MessageMapping("challenger.getRoomState")
    public Mono<RoomGameStateVO> getRoomState(@Payload GetRoomStateRequest request) {
        Long roomId = request.getRoomId();
        log.info("获取房间状态，房间：{}", roomId);
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        RoomGameStateVO roomGameStateVO = BeanUtil.copyProperties(roomGameState, RoomGameStateVO.class);
        return Mono.just(roomGameStateVO);
    }

    @MessageMapping("challenger.buildDeck")
    public Mono<List<CardInstance>> buildDeck(@Payload BuildDeckRequest request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();
        Integer optionId = request.getOptionId();
        Set<Integer> selectedCardInstanceIds = request.getSelectedCardInstanceIds();
        log.info("构建牌组，房间：{}，用户：{}，选项：{}", roomId, userId, optionId);
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        LinkedList<CardInstance> cardInstances = 
                roomGameState.buildCardInstances(userId, optionId, selectedCardInstanceIds);
        return Mono.just(cardInstances);
    }

    @MessageMapping("challenger.readyBattle")
    public Mono<Void> readyBattle(@Payload ReadyBattleRequest request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();
        String battlefieldName = request.getBattlefieldName();
        log.info("准备战斗，房间：{}，用户：{}，战场：{}", roomId, userId, battlefieldName);
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        roomGameState.readyBattle(battlefieldName, userId);
        return Mono.empty();
    }

    @MessageMapping("challenger.discardCard")
    public Mono<Void> discardCard(@Payload DiscardCardRequest request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();
        Set<Integer> cardInstanceIds = request.getCardInstanceIds();
        log.info("弃牌，房间：{}，用户：{}，卡牌数：{}", roomId, userId, cardInstanceIds.size());
        
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        ThrowUtils.throwIf(roomGameState == null, ErrorCode.NOT_FOUND_ERROR, "房间不存在");
        
        roomGameState.discardCardInstances(userId, cardInstanceIds);
        return Mono.empty();
    }

    @MessageMapping("challenger.cardSelect")
    public Mono<Void> onPlayerSelectCard(@Payload CardSelectorResponse cardSelectorResponse) {
        String visitorId = cardSelectorResponse.getUserId();
        log.info("用户 {} 选择卡牌", visitorId);
        String waitKey = "user_" + visitorId;
        playerWaitManager.completeWait(waitKey, cardSelectorResponse);
        return Mono.empty();
    }

    @MessageMapping("challenger.checkAndPut")
    public Mono<Void> onPlayerCheckAndPut(@Payload CheckAndPutResponse checkAndPutResponse) {
        String visitorId = checkAndPutResponse.getUserId();
        log.info("用户 {} 查看并放置卡牌", visitorId);
        String waitKey = "user_" + visitorId;
        playerWaitManager.completeWait(waitKey, checkAndPutResponse);
        return Mono.empty();
    }

    // ==================== 服务端推送方法（供 EventListener 调用）====================

    public void notifyPlayerWaitOpponent(Long userId, Long opponentId) {
        GameMessage gameMessage = new GameMessage();
        gameMessage.setType(ChallengerMessageTypeEnum.WAIT_OPPONENT_READY.getValue());
        gameMessage.setBody(String.valueOf(opponentId));
        rSocketGameHandler.sendChallengerMessage(userId, gameMessage);
    }

    public void notifyPlayerStartBattle(Long userId, Long opponentId, StartBattleResponse startBattleResponse) {
        GameMessage gameMessage = new GameMessage();
        gameMessage.setType(ChallengerMessageTypeEnum.START_BATTLE.getValue());
        gameMessage.setBody(JSONUtil.toJsonStr(startBattleResponse));
        rSocketGameHandler.sendChallengerMessage(userId, gameMessage);
    }

    public void notifyPlayerCardSelect(Long userId, CardSelectorRequest cardSelectorRequest) {
        GameMessage gameMessage = new GameMessage();
        gameMessage.setType(ChallengerMessageTypeEnum.SELECT_CARD.getValue());
        gameMessage.setBody(JSONUtil.toJsonStr(cardSelectorRequest));
        rSocketGameHandler.sendChallengerMessage(userId, gameMessage);
    }

    public void notifyPlayerCheckAndPut(Long userId, CheckAndPutRequest checkAndPutRequest) {
        GameMessage gameMessage = new GameMessage();
        gameMessage.setType(ChallengerMessageTypeEnum.CHECK_AND_PUT.getValue());
        gameMessage.setBody(JSONUtil.toJsonStr(checkAndPutRequest));
        rSocketGameHandler.sendChallengerMessage(userId, gameMessage);
    }
}
