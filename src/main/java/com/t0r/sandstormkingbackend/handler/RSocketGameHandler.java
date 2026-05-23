package com.t0r.sandstormkingbackend.handler;

import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.model.dto.game.GameMessage;
import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import com.t0r.sandstormkingbackend.model.dto.rSocket.ForwardedMessageRequest;
import com.t0r.sandstormkingbackend.model.dto.room.RoomRSocketRequest;
import com.t0r.sandstormkingbackend.model.enums.WSMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO 安全传递 userId
 */
@Controller
@Slf4j
public class RSocketGameHandler {

    private final Map<Long, Long> roomOwnerId = new ConcurrentHashMap<>();
    // key: roomId, value: set of userIds
    private final Map<Long, Set<Long>> roomPlayers = new ConcurrentHashMap<>();
    // key: userId, value: requester
    private final Map<Long, RSocketRequester> userRequesters = new ConcurrentHashMap<>();

    private final Map<Long, Sinks.Many<WSMessage>> userSinks = new ConcurrentHashMap<>();

    // TODO 后面修改
    private final Map<Long, Sinks.Many<GameMessage>> challengerSinks = new ConcurrentHashMap<>();

    @ConnectMapping
    public void onConnect(RSocketRequester requester, @Payload Long userId) {
        log.info("RSocket 连接建立，用户：{}", userId);
        userRequesters.put(userId, requester);

        // 监听断开
        Objects.requireNonNull(requester.rsocket())
                .onClose()
                .doOnTerminate(() -> {
                    userRequesters.remove(userId);
                    userSinks.remove(userId);
                    challengerSinks.remove(userId);
                })
                .subscribe();
    }

    @MessageMapping("game.joinRoom")
    public Mono<Void> joinRoom(RoomRSocketRequest roomRSocketRequest) {
        Long userId = roomRSocketRequest.getUser().getId();
        String userName = roomRSocketRequest.getUser().getUserName();
        Long roomId = roomRSocketRequest.getRoomId();
        log.info("用户：{}，创建/加入房间 RSocket 服务，房间：{}", userId, roomId);
        ThrowUtils.throwIf(userRequesters.get(userId) == null,
                ErrorCode.NOT_FOUND_ERROR, "用户未连接至 RSocket 服务");

        Long ownerId = roomOwnerId.get(roomId);
        if (ownerId == null) { // 创建房间
            roomOwnerId.put(roomId, userId);
            roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        } else { // 加入房间
            // 构造响应
            WSMessage wsMessage = new WSMessage();
            wsMessage.setType(WSMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
            String message = String.format("%s加入房间", userName);
            wsMessage.setDescription(message);

            broadcast(roomId, wsMessage);
        }
        roomPlayers.get(roomId).add(userId);

        return Mono.empty();
    }

    @MessageMapping("game.quitRoom")
    public Mono<Void> quitRoom(RoomRSocketRequest roomRSocketRequest) {
        Long userId = roomRSocketRequest.getUser().getId();
        String userName = roomRSocketRequest.getUser().getUserName();
        Long roomId = roomRSocketRequest.getRoomId();

        Set<Long> players = roomPlayers.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
        players.remove(userId);

        // 广播离开消息
        WSMessage wsMessage = new WSMessage();
        wsMessage.setType(WSMessageTypeEnum.ROOM_STATE_CHANGED.getValue());
        wsMessage.setDescription(userName + "离开房间");
        broadcast(roomId, wsMessage);

        return Mono.empty();
    }

    // TODO 暴露给Controller用
    public void broadcast(Long roomId, WSMessage wsMessage, Long excludeUserId) {
        log.info("广播消息，房间：{}", roomId);
        Set<Long> userIds = roomPlayers.get(roomId);
        if (userIds == null)
            return;
        for (Long userId : userIds) {
            if (userId.equals(excludeUserId))
                continue;
            Sinks.Many<WSMessage> sink = userSinks.get(userId);
            if (sink != null) {
                sink.tryEmitNext(wsMessage);
            }
        }
    }

    public void broadcast(Long roomId, WSMessage wsMessage) {
        broadcast(roomId, wsMessage, null);
    }

    public void sendToUser(Long userId, WSMessage wsMessage) {
        log.info("接受用户：{}，发送消息：{}", userId, wsMessage);
        Sinks.Many<WSMessage> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(wsMessage);
        }
    }

    public void sendChallengerMessage(Long userId, GameMessage gameMessage) {
        log.info("接受用户：{}，发送挑战消息：{}", userId, gameMessage);
        Sinks.Many<GameMessage> sink = challengerSinks.computeIfAbsent(userId,
                id -> Sinks.many().multicast().onBackpressureBuffer());
        sink.tryEmitNext(gameMessage);
    }

    @MessageMapping("game.receive")
    public Flux<WSMessage> handleReceiveStream(@Payload Long userId) {
        log.info("用户：{}，准备接收消息", userId);
        Sinks.Many<WSMessage> sink = userSinks.computeIfAbsent(userId,
                id -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }

    @MessageMapping("message")
    public Mono<Void> handleMessage(ForwardedMessageRequest forwardedMessageRequest) {
        Long userId = forwardedMessageRequest.getUserId();
        Long roomId = forwardedMessageRequest.getRoomId();
        WSMessage wsMessage = forwardedMessageRequest.getWsMessage();
        log.info("用户：{}，发送并转发到所属房间消息：{}", userId, wsMessage);
        broadcast(roomId, wsMessage, userId);
        return Mono.empty();
    }

}