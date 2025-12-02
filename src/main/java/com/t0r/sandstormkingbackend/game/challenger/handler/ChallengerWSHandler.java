package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.ConfirmChoiceRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.RoomInitRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.GameMessageTypeEnum;
import com.t0r.sandstormkingbackend.model.enums.MessageBroadcastTypeEnum;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.Set;

@Slf4j
@Component
public class ChallengerWSHandler {

    @Resource
    private ChallengerGameManager challengerGameManager;

    public MessageBroadcastTypeEnum handleMessage(GameMessage gameMessage, WebSocketSession session,
                                                  User user, Long roomId) throws Exception {
        String type = gameMessage.getType();
        GameMessageTypeEnum gameMessageTypeEnum = GameMessageTypeEnum.valueOf(type);
        switch (gameMessageTypeEnum) {
            case START_GAME:
                return handleStartGameMessage(gameMessage);
            case DRAW_CARD:
            case DRAW_AGAIN:
                return handleDrawCardMessage(gameMessage, roomId, user.getId());
            case CONFIRM_CHOICE:
                return handleConfirmChoiceMessage(gameMessage, roomId, user.getId());
            case READY_BATTLE:
                return handleReadyBattleMessage(gameMessage, roomId, user.getId());
        }
        return null;
    }

    private MessageBroadcastTypeEnum handleReadyBattleMessage(GameMessage gameMessage, Long roomId, Long userId) {
        String battlefield = gameMessage.getBody();
        Long opponentId = challengerGameManager.readyBattle(roomId, userId, battlefield);
        gameMessage.getUserIds().add(opponentId);
        return MessageBroadcastTypeEnum.CUSTOM;
    }

    private MessageBroadcastTypeEnum handleConfirmChoiceMessage(GameMessage gameMessage, Long roomId, Long userId) {
        String body = gameMessage.getBody();
        ConfirmChoiceRequest confirmChoiceRequest = JSONUtil.toBean(body, ConfirmChoiceRequest.class);
        Integer optionId = confirmChoiceRequest.getOptionId();
        Set<Integer> selectedCardInstanceIds = confirmChoiceRequest.getSelectedCardInstanceIds();
        boolean isConfirm = challengerGameManager.confirmSelect(roomId, userId, optionId, selectedCardInstanceIds);
        gameMessage.setDescription("成功选择");
        gameMessage.setBody(JSONUtil.toJsonStr(isConfirm));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleDrawCardMessage(GameMessage gameMessage, Long roomId, Long userId) {
        String body = gameMessage.getBody();
        Integer OptionId = Integer.parseInt(body);
        LinkedList<CardInstance> cardInstances = challengerGameManager.buildCardInstances(roomId, userId, OptionId);
        gameMessage.setBody(JSONUtil.toJsonStr(cardInstances));
        return MessageBroadcastTypeEnum.SELF;
    }

    private MessageBroadcastTypeEnum handleStartGameMessage(GameMessage gameMessage) throws Exception {
        String body = gameMessage.getBody();
        RoomInitRequest roomInitRequest = JSONUtil.toBean(body, RoomInitRequest.class);
        challengerGameManager.startGame(roomInitRequest);
        return MessageBroadcastTypeEnum.ALL;
    }


}
