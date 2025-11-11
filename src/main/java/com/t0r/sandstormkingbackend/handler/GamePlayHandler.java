package com.t0r.sandstormkingbackend.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sun.istack.internal.NotNull;
import com.t0r.sandstormkingbackend.model.dto.game.WebSocketRequestMessage;
import com.t0r.sandstormkingbackend.model.dto.game.WebSocketResponseMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.PlayerActionEnum;
import com.t0r.sandstormkingbackend.model.enums.WebSocketMessageTypeEnum;
import com.t0r.sandstormkingbackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GamePlayHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    // todo 先写死房间人数
    private final int MAX_PLAYERS = 2;

    private final Map<Long, Long> roomOwnerId = new ConcurrentHashMap<>();

    // 每个房间的人数，key: roomId, value: 当前房间人数
    private final Map<Long, Integer> roomPlayerCounts = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: 房间 ID, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> playerSessions = new ConcurrentHashMap<>();

    private void broadcastToPlayers(Long roomId,
                                    WebSocketResponseMessage webSocketResponseMessage,
                                    WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = playerSessions.get(roomId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(webSocketResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    // 全部广播
    private void broadcastToPlayers(Long roomId,
                                    WebSocketResponseMessage webSocketResponseMessage) throws Exception {
        this.broadcastToPlayers(roomId, webSocketResponseMessage, null);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // todo 重构代码，整理创建房间和加入房间时判断房间人数和权限的逻辑
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long roomId = (Long) session.getAttributes().get("roomId");
        playerSessions.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        playerSessions.get(roomId).add(session);

        // 构造响应
        WebSocketResponseMessage webSocketResponseMessage = new WebSocketResponseMessage();
        webSocketResponseMessage.setType(WebSocketMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入房间", user.getUserName());
        webSocketResponseMessage.setMessage(message);
        webSocketResponseMessage.setUser(userService.getUserVO(user));

        // 广播给同一房间的玩家
        broadcastToPlayers(roomId, webSocketResponseMessage);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PlayerAction
        WebSocketRequestMessage webSocketRequestMessage = JSONUtil.toBean(message.getPayload(), WebSocketRequestMessage.class);
        String type = webSocketRequestMessage.getType();
        WebSocketMessageTypeEnum webSocketMessageTypeEnum = WebSocketMessageTypeEnum.valueOf(type);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long roomId = (Long) attributes.get("roomId");

        // 调用对应的消息处理方法
        switch (webSocketMessageTypeEnum) {
            case JOIN_ROOM:
                handleJoinRoomMessage(webSocketRequestMessage, session, user, roomId);
                break;
            case LEAVE_ROOM:
                handleLeaveRoomMessage(webSocketRequestMessage, session, user, roomId);
                break;
            case START_GAME:
                // todo 补充函数
//                handleStartGameMessage(webSocketRequestMessage, session, user, roomId);
                break;
            case GAME_STATE:
                // todo 补充函数
//                handleGameStateMessage(webSocketRequestMessage, session, user, roomId);
                break;
            case GAME_OVER:
                // todo 补充函数
//                handleGameOverMessage(webSocketRequestMessage, session, user, roomId);
                break;
            case PLAYER_ACTION:
                handlePlayerActionMessage(webSocketRequestMessage, session, user, roomId);
                break;
            default:
                WebSocketResponseMessage webSocketResponseMessage = new WebSocketResponseMessage();
                webSocketResponseMessage.setType(WebSocketMessageTypeEnum.ERROR.getValue());
                webSocketResponseMessage.setMessage("消息类型错误");
                webSocketResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(webSocketResponseMessage)));
        }
    }

    public void handlePlayerActionMessage(WebSocketRequestMessage webSocketRequestMessage,
                                          WebSocketSession session,
                                          User user, Long pictureId) throws Exception {
        // todo 这里的 data 还要处理
        String data = webSocketRequestMessage.getData();
        PlayerActionEnum actionEnum = PlayerActionEnum.getEnumByValue(data);
        if (actionEnum == null) {
            return;
        }

        WebSocketResponseMessage pictureEditResponseMessage = new WebSocketResponseMessage();
        pictureEditResponseMessage.setType(WebSocketMessageTypeEnum.PLAYER_ACTION.getValue());
        String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
        pictureEditResponseMessage.setMessage(message);
        // todo 也要处理
        pictureEditResponseMessage.setPlayerAction(data);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给除了当前客户端之外的其他用户，否则会造成重复操作
        broadcastToPlayers(pictureId, pictureEditResponseMessage, session);
    }

    public void handleJoinRoomMessage(WebSocketRequestMessage webSocketRequestMessage,
                                      WebSocketSession session,
                                      User user, Long roomId) throws Exception {
        // 当前房间人数小于最大值，才能进入
        if (roomPlayerCounts.get(roomId) < MAX_PLAYERS) {
            roomPlayerCounts.put(roomId, roomPlayerCounts.get(roomId) + 1);

            WebSocketResponseMessage webSocketResponseMessage = new WebSocketResponseMessage();
            webSocketResponseMessage.setType(WebSocketMessageTypeEnum.JOIN_ROOM.getValue());
            String message = String.format("%s进入房间", user.getUserName());
            webSocketResponseMessage.setMessage(message);
            webSocketResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPlayers(roomId, webSocketResponseMessage);
        }
    }

    public void handleLeaveRoomMessage(WebSocketRequestMessage webSocketRequestMessage,
                                       WebSocketSession session,
                                       User user, Long roomId) throws Exception {
        Integer currentRoomPlayerCount = roomPlayerCounts.get(roomId);
        if (currentRoomPlayerCount != null) {
            roomPlayerCounts.put(roomId, roomPlayerCounts.get(roomId) - 1);

            WebSocketResponseMessage webSocketResponseMessage = new WebSocketResponseMessage();
            webSocketResponseMessage.setType(WebSocketMessageTypeEnum.LEAVE_ROOM.getValue());
            String message = String.format("%s离开房间", user.getUserName());
            webSocketResponseMessage.setMessage(message);
            webSocketResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPlayers(roomId, webSocketResponseMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long roomId = (Long) attributes.get("roomId");
        User user = (User) attributes.get("user");
        // 离开房间
        // todo 可能是变成断开连接状态
        handleLeaveRoomMessage(null, session, user, roomId);

        // 删除会话
        Set<WebSocketSession> sessionSet = playerSessions.get(roomId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                playerSessions.remove(roomId);
            }
        }

        WebSocketResponseMessage webSocketResponseMessage = new WebSocketResponseMessage();
        webSocketResponseMessage.setType(WebSocketMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开房间", user.getUserName());
        webSocketResponseMessage.setMessage(message);
        webSocketResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPlayers(roomId, webSocketResponseMessage);
    }



}
