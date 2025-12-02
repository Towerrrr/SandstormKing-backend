package com.t0r.sandstormkingbackend.handler;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.MessageBroadcastTypeEnum;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

public class BroadcastUtil {

    public static void sendMessage(MessageBroadcastTypeEnum messageBroadcastTypeEnum,
                                   Set<WebSocketSession> sessionSet,
                                   WSMessage wsMessage,
                                   WebSocketSession selfSession) throws Exception {
        switch (messageBroadcastTypeEnum) {
            case ALL:
                broadcastExclude(sessionSet, wsMessage, null);
                break;
            case OTHERS:
                broadcastExclude(sessionSet, wsMessage, selfSession);
                break;
            case SELF:
                broadcastToSelf(selfSession, wsMessage);
                break;
            case CUSTOM:
                broadcastCustom(sessionSet, wsMessage);
                break;
        }
    }

    private static void broadcastToSelf(WebSocketSession selfSession, WSMessage wsMessage) throws Exception {
        if (selfSession != null && selfSession.isOpen()) {
            TextMessage textMessage = buildTextMessage(wsMessage);
            selfSession.sendMessage(textMessage);
        }
    }

    private static void broadcastCustom(Set<WebSocketSession> sessionSet, WSMessage wsMessage) throws Exception {
        if (CollUtil.isNotEmpty(sessionSet)) {
            Set<Long> userIds = wsMessage.getGameMessage().getUserIds();
            for (WebSocketSession session : sessionSet) {
                User user = (User) session.getAttributes().get("user");
                Long userId = user.getId();
                if (userIds.contains(userId) && session.isOpen()) {
                    session.sendMessage(buildTextMessage(wsMessage));
                }
            }
        }
    }

    private static void broadcastExclude(Set<WebSocketSession> sessionSet, WSMessage wsMessage,
                                         WebSocketSession excludeSession) throws Exception {
        if (CollUtil.isNotEmpty(sessionSet)) {
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(buildTextMessage(wsMessage));
                }
            }
        }
    }

    private static TextMessage buildTextMessage(WSMessage wsMessage) throws Exception {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
        objectMapper.registerModule(module);
        // 序列化为 JSON 字符串
        String message = objectMapper.writeValueAsString(wsMessage);
        return new TextMessage(message);
    }

}
