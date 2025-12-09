package com.t0r.sandstormkingbackend.handler;

import cn.hutool.json.JSONUtil;
import com.sun.istack.internal.NotNull;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerWSHandler;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.MessageBroadcastTypeEnum;
import com.t0r.sandstormkingbackend.model.enums.WSMessageTypeEnum;
import com.t0r.sandstormkingbackend.service.RoomService;
import com.t0r.sandstormkingbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GamePlayHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private RoomService roomService;

    @Resource
    private ChallengerWSHandler challengerWSHandler;

    private final Map<Long, Long> roomOwnerId = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: 房间 ID, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> playerSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long roomId = (Long) session.getAttributes().get("roomId");

        log.info("WebSocket连接中，用户：{}，房间：{}", user.getUserName(), roomId);

        Long ownerId = roomOwnerId.get(roomId);
        if (ownerId == null) { // 创建房间
            roomOwnerId.put(roomId, user.getId());
            playerSessions.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        } else { // 加入房间
            // 构造响应
            WSMessage wsMessage = new WSMessage();
            wsMessage.setType(WSMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
            String message = String.format("%s加入房间", user.getUserName());
            wsMessage.setDescription(message);

            BroadcastUtil.sendMessage(MessageBroadcastTypeEnum.ALL, playerSessions.get(roomId), wsMessage, session);
        }
        playerSessions.get(roomId).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WSMessage wsMessage = JSONUtil.toBean(message.getPayload(), WSMessage.class);
        String type = wsMessage.getType();
        WSMessageTypeEnum wsMessageTypeEnum = WSMessageTypeEnum.valueOf(type);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long roomId = (Long) attributes.get("roomId");

        Set<WebSocketSession> webSocketSessions = playerSessions.get(roomId);

        switch (wsMessageTypeEnum) {
            case ROOM_STATE_CHANGED:
            case START_GAME:
                BroadcastUtil.sendMessage(MessageBroadcastTypeEnum.ALL, playerSessions.get(roomId), wsMessage, session);
                challengerWSHandler.handleMessage(wsMessage.getGameMessage(), session, webSocketSessions, user, roomId);
                BroadcastUtil.sendMessage(MessageBroadcastTypeEnum.ALL, playerSessions.get(roomId), wsMessage, session);
                break;
            case CHALLENGER:
                MessageBroadcastTypeEnum messageBroadcastTypeEnum2 =
                        challengerWSHandler.handleMessage(wsMessage.getGameMessage(), session, webSocketSessions, user, roomId);
                BroadcastUtil.sendMessage(messageBroadcastTypeEnum2, playerSessions.get(roomId), wsMessage, session);
                break;
            default:
                wsMessage.setType(WSMessageTypeEnum.ERROR.getValue());
                wsMessage.setDescription("消息类型错误");
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long roomId = (Long) attributes.get("roomId");
        User user = (User) attributes.get("user");
        // todo 可能是变成断开连接状态

        // 删除会话
        Set<WebSocketSession> sessionSet = playerSessions.get(roomId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                playerSessions.remove(roomId);
            }
        }

        // 判断是否是房主退出房间
        Long ownerId = roomOwnerId.get(roomId);
        if (ownerId != null && ownerId.equals(user.getId())) {
            Room room = roomService.getById(roomId);
            Long newOwnerId = room.getOwnerId();
            if (newOwnerId != null && !newOwnerId.equals(ownerId)) {
                roomOwnerId.put(roomId, newOwnerId);

                WSMessage ownerChangedMsg = new WSMessage();
                ownerChangedMsg.setType(WSMessageTypeEnum.INFO.getValue());
                ownerChangedMsg.setDescription("房主已变更");
                BroadcastUtil.sendMessage(MessageBroadcastTypeEnum.ALL, playerSessions.get(roomId), ownerChangedMsg, session);
            }
        }

        WSMessage wsMessage = new WSMessage();
        wsMessage.setType(WSMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
        String message = String.format("%s离开房间", user.getUserName());
        wsMessage.setDescription(message);
        BroadcastUtil.sendMessage(MessageBroadcastTypeEnum.ALL, playerSessions.get(roomId), wsMessage, session);
    }


}
