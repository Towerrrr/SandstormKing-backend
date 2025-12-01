package com.t0r.sandstormkingbackend.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sun.istack.internal.NotNull;
import com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerWSHandler;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.WebSocketMessageTypeEnum;
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

    private void broadcastToPlayers(Long roomId,
                                    WSMessage wsMessage,
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
            String message = objectMapper.writeValueAsString(wsMessage);
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

    /**
     * 全部广播
     */
    private void broadcastToPlayers(Long roomId, WSMessage wsMessage) throws Exception {
        this.broadcastToPlayers(roomId, wsMessage, null);
    }

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
            wsMessage.setType(WebSocketMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
            String message = String.format("%s加入房间", user.getUserName());
            wsMessage.setDescription(message);

            // 广播给同一房间的玩家
            broadcastToPlayers(roomId, wsMessage);
        }
        playerSessions.get(roomId).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WSMessage wsMessage = JSONUtil.toBean(message.getPayload(), WSMessage.class);
        String type = wsMessage.getType();
        WebSocketMessageTypeEnum webSocketMessageTypeEnum = WebSocketMessageTypeEnum.valueOf(type);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long roomId = (Long) attributes.get("roomId");

        // 调用对应的消息处理方法
        switch (webSocketMessageTypeEnum) {
            case ROOM_STATE_CHANGED:
                handleRoomStateChangedMessage(wsMessage, session, user, roomId);
                break;
            case CHALLENGER:
                challengerWSHandler.handleMessage(wsMessage.getGameMessage(), session, user, roomId);
                // TODO 测试一下这样消息能否修改
                broadcastToPlayers(roomId, wsMessage);
                break;
            default:
                wsMessage.setType(WebSocketMessageTypeEnum.ERROR.getValue());
                wsMessage.setDescription("消息类型错误");
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
        }
    }

    private void handleRoomStateChangedMessage(WSMessage wsMessage,
                                               WebSocketSession session, User user, Long roomId) throws Exception {
        wsMessage.setType(WebSocketMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
        wsMessage.setDescription("房间状态变更");
        broadcastToPlayers(roomId, wsMessage);
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
                ownerChangedMsg.setType(WebSocketMessageTypeEnum.INFO.getValue());
                ownerChangedMsg.setDescription("房主已变更");
                broadcastToPlayers(roomId, ownerChangedMsg);
            }
        }

        WSMessage wsMessage = new WSMessage();
        wsMessage.setType(WebSocketMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
        String message = String.format("%s离开房间", user.getUserName());
        wsMessage.setDescription(message);
        broadcastToPlayers(roomId, wsMessage);
    }


}
