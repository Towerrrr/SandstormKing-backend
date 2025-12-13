package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.BuildDeckRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.InitGameRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.RoomGameState;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Battlefield;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.ChallengerMessageTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.BattlefieldVO;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.ChallengerPlayerSelf;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.ChallengerPlayerVO;
import com.t0r.sandstormkingbackend.game.challenger.model.vo.RoomGameStateVO;
import com.t0r.sandstormkingbackend.model.enums.MessageBroadcastTypeEnum;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ChallengerWSHandler {

    @Resource
    private ChallengerGameManager challengerGameManager;

    public MessageBroadcastTypeEnum handleMessage(GameMessage gameMessage, WebSocketSession session,
                                                  Set<WebSocketSession> webSocketSessions,
                                                  User user, Long roomId) throws Exception {
        String type = gameMessage.getType();
        ChallengerMessageTypeEnum challengerMessageTypeEnum = ChallengerMessageTypeEnum.valueOf(type);
        switch (challengerMessageTypeEnum) {
            case INIT_GAME:
                return handleInitGameMessage(gameMessage, webSocketSessions);
            case GET_PLAYER:
                // TODO 先粗暴地让前端请求资源，后续再优化
                return handleGetPlayerMessage(gameMessage, roomId, user.getId());
            case GET_PLAYER_VO:
                return handleGetPlayerVOMessage(gameMessage, roomId);
            case GET_BATTLEFIELD:
                return handleGetBattlefieldMessage(gameMessage, roomId);
            case GET_ROOM_STATE:
                return handleGetRoomStateMessage(gameMessage, roomId);
            case BUILD_DECK:
                return handleBuildDeckMessage(gameMessage, roomId, user.getId());
            case READY_BATTLE:
                handleReadyBattleMessage(gameMessage, roomId, user.getId());
                break;
            case DISCARD_CARD:
                // 前端在开战前将之前所有丢弃的卡牌保存，到开战前才调用丢弃api
                return handleDiscardCardMessage(gameMessage, roomId, user.getId());
        }
        return null;
    }

    private MessageBroadcastTypeEnum handleGetRoomStateMessage(GameMessage gameMessage, Long roomId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        RoomGameStateVO roomGameStateVO = BeanUtil.copyProperties(roomGameState, RoomGameStateVO.class);
        gameMessage.setBody(JSONUtil.toJsonStr(roomGameStateVO));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleGetBattlefieldMessage(GameMessage gameMessage, Long roomId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);

        String battlefieldName = gameMessage.getBody();
        // TODO 这一层所有方法有参数都要在这里做校验
        ThrowUtils.throwIf(battlefieldName == null, ErrorCode.PARAMS_ERROR, "请选择战斗场");
        Battlefield battlefield = roomGameState.getTempBattlefields().get(battlefieldName);
        gameMessage.setBody(JSONUtil.toJsonStr(new BattlefieldVO(battlefield)));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleDiscardCardMessage(GameMessage gameMessage, Long roomId, Long userId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);

        String body = gameMessage.getBody();
        Set<Integer> cardInstanceIds = new HashSet<>(JSONUtil.toList(body, Integer.class));
        roomGameState.discardCardInstances(userId, cardInstanceIds);
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleGetPlayerMessage(GameMessage gameMessage, Long roomId, Long userId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);

        // TODO 后续考虑优化一个 ChallengerPlayerVO
        ChallengerPlayer challengerPlayer = roomGameState.getChallengerPlayers().get(userId);
        gameMessage.setBody(JSONUtil.toJsonStr(new ChallengerPlayerSelf(challengerPlayer)));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleGetPlayerVOMessage(GameMessage gameMessage, Long roomId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);

        long userId = Long.parseLong(gameMessage.getBody());
        ChallengerPlayer challengerPlayer = roomGameState.getChallengerPlayers().get(userId);
        gameMessage.setBody(JSONUtil.toJsonStr(new ChallengerPlayerVO(challengerPlayer)));
        return MessageBroadcastTypeEnum.SELF;
    }

    private void handleReadyBattleMessage(GameMessage gameMessage, Long roomId, Long userId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);
        String battlefield = gameMessage.getBody();
        roomGameState.readyBattle(battlefield, userId);
    }

    private MessageBroadcastTypeEnum handleBuildDeckMessage(GameMessage gameMessage, Long roomId, Long userId) {
        RoomGameState roomGameState = challengerGameManager.getRoomGameStateMap().get(roomId);

        String body = gameMessage.getBody();
        BuildDeckRequest buildDeckRequest = JSONUtil.toBean(body, BuildDeckRequest.class);
        Integer optionId = buildDeckRequest.getOptionId();
        Set<Integer> selectedCardInstanceIds = buildDeckRequest.getSelectedCardInstanceIds();
        LinkedList<CardInstance> cardInstances =
                roomGameState.buildCardInstances(userId, optionId, selectedCardInstanceIds);
        gameMessage.setBody(JSONUtil.toJsonStr(cardInstances));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleInitGameMessage(GameMessage gameMessage, Set<WebSocketSession> webSocketSessions) throws Exception {
        String body = gameMessage.getBody();
        InitGameRequest initGameRequest = JSONUtil.toBean(body, InitGameRequest.class);
        Map<Integer, Card> cardMap = challengerGameManager.initGame(initGameRequest, webSocketSessions);
        gameMessage.setBody(JSONUtil.toJsonStr(cardMap));
        return MessageBroadcastTypeEnum.ALL;
    }


}
