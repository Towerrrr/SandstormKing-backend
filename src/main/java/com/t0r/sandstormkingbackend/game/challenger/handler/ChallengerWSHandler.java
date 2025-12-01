package com.t0r.sandstormkingbackend.game.challenger.handler;

import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.dto.RoomInitRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.GameMessageTypeEnum;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.WebSocketMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Slf4j
@Component
public class ChallengerWSHandler {

    @Resource
    private ChallengerGameManager challengerGameManager;

    public void handleMessage(GameMessage gameMessage, WebSocketSession session,
                                     User user, Long roomId) throws Exception {
        String type = gameMessage.getType();
        GameMessageTypeEnum gameMessageTypeEnum = GameMessageTypeEnum.valueOf(type);
        switch (gameMessageTypeEnum) {
            case START_GAME:
                handleStartGameMessage(gameMessage);
                break;
            case END_GAME:
        }

    }

    private void handleStartGameMessage(GameMessage gameMessage) throws Exception {
        String request = gameMessage.getRequest();
        RoomInitRequest roomInitRequest = JSONUtil.toBean(request, RoomInitRequest.class);
        challengerGameManager.startGame(roomInitRequest);
        gameMessage.setType(WebSocketMessageTypeEnum.START_GAME.getValue());
        gameMessage.setDescription("游戏开始");
    }


}
