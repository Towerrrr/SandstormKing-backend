package com.t0r.sandstormkingbackend.handler;

import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.model.enums.WSMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@Slf4j
public class RSocketGameHandler {

    private final Map<Long, Long> roomOwnerId = new ConcurrentHashMap<>();
    // key: roomId, value: set of userIds
    private final Map<Long, Set<Long>> roomPlayers = new ConcurrentHashMap<>();
    // key: userId, value: requester
    private final Map<Long, RSocketRequester> userRequesters = new ConcurrentHashMap<>();

    @ConnectMapping("game.connect")
    public void onConnect(RSocketRequester requester, WebSession session) {
        User user = session.getAttribute("user");
        Long roomId = session.getAttribute("roomId");
        log.info("WebSocket连接中，用户：{}，房间：{}", Objects.requireNonNull(user).getId(), roomId);

        Long ownerId = roomOwnerId.get(roomId);
        if (ownerId == null) { // 创建房间
            roomOwnerId.put(roomId, user.getId());
            roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        } else { // 加入房间
            // 构造响应
            WSMessage wsMessage = new WSMessage();
            wsMessage.setType(WSMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
            String message = String.format("%s加入房间", user.getUserName());
            wsMessage.setDescription(message);

            // TODO 差一个通用的广播方法
        }
        roomPlayers.get(roomId).add(user.getId());
        userRequesters.put(user.getId(), requester);

        // 监听断开
        requester.rsocket()
                .onClose()
                .doOnTerminate(() -> {
                    roomPlayers.getOrDefault(roomId, ConcurrentHashMap.newKeySet()).remove(userId);
                    userRequesters.remove(userId);
                    // TODO 广播用户离开
//                    broadcast(roomId, new WSMessage("ROOM_STATE_CHANGED", userId + " 离开房间"));
                })
                .subscribe();
    }

    @MessageMapping("message")
    public Flux<String> handleMessage(String message) {
        log.info("Received message: {}", message);
        return Flux.just("Echo: " + message);
    }
}