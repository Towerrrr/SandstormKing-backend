package com.t0r.sandstormkingbackend.service.impl;

import cn.hutool.json.JSONUtil;
import com.t0r.sandstormkingbackend.common.PageRequest;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.model.dto.room.ReadyRequest;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.RoomMember;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.t0r.sandstormkingbackend.model.entity.Room.convertRoomToMap;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Boolean ready(ReadyRequest readyRequest, User loginUser) {
        Long roomId = readyRequest.getRoomId();
        boolean ready = readyRequest.isReady();

        String roomKey = "room:" + roomId;
        String playersKey = "room:" + roomId + ":members";
        String userIdStr = String.valueOf(loginUser.getId());
        String roomMemberStr = (String) redisTemplate.opsForHash().get(playersKey, userIdStr);
        RoomMember roomMember = JSONUtil.toBean(roomMemberStr, RoomMember.class);
        roomMember.setReady(ready);
        redisTemplate.opsForHash().put(playersKey, userIdStr, JSONUtil.toJsonStr(roomMember));
        return true;
    }

    @Override
    public Boolean startGame(Long roomId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "房间ID无效");

        boolean isOwner = loginUser.getId().equals(getById(roomId).getOwnerId());
        ThrowUtils.throwIf(!isOwner, ErrorCode.FORBIDDEN_ERROR, "你不是房主，不能开始游戏");

        String playersKey = "room:" + roomId + ":members";
        Map<Object, Object> playerMap = redisTemplate.opsForHash().entries(playersKey);

        List<RoomMember> roomMembers = playerMap.values().stream()
                .map(obj -> JSONUtil.toBean(String.valueOf(obj), RoomMember.class))
                .peek(member -> {
                    if (!member.getReady()) {
                        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "房间内有玩家未准备好");
                    }
                })
                .collect(Collectors.toList());

        log.info("开始游戏，房间ID：{}，房主ID：{}", roomId, loginUser.getId());
        // todo 开始游戏

        return true;
    }

    @Override
    public Room getById(Long roomId) {
        String roomKey = "room:" + roomId;
        Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);
        return convertMapToRoom(roomMap);
    }

    @Override
    public List<Room> listRooms(PageRequest pageRequest) {
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();

        long start = (long) (current - 1) * pageSize;
        long end = start + pageSize - 1;

        Set<Object> roomIdSet = redisTemplate.opsForZSet().reverseRange("room:list", start, end);
        List<Room> roomList = new ArrayList<>();
        if (roomIdSet != null) {
            for (Object roomId : roomIdSet) {
                String roomKey = "room:" + roomId;
                Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);
                Room room = convertMapToRoom(roomMap);
                roomList.add(room);
            }
        }
        return roomList;
    }

    @Override
    public boolean quitRoom(Long roomId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR, "房间ID无效");

        String roomKey = "room:" + roomId;
        String playersKey = "room:" + roomId + ":members";

        Boolean hasRoom = redisTemplate.hasKey(roomKey);
        ThrowUtils.throwIf(Boolean.FALSE.equals(hasRoom), ErrorCode.NOT_FOUND_ERROR, "房间不存在");

        Long removedCount = redisTemplate.opsForHash().delete(playersKey, String.valueOf(loginUser.getId()));
        if (removedCount == 0) {
            // 用户本来就不在房间
            return false;
        }

        // 检查用户是否为房主
        Object ownerIdObj = redisTemplate.opsForHash().get(roomKey, Room.OWNER_ID);
        Long ownerId = ownerIdObj != null ? Long.valueOf(ownerIdObj.toString()) : null;

        boolean isOwner = loginUser.getId().equals(ownerId);

        // 获取退出后的所有成员（用 hash 的 keys 判断）
        Map<Object, Object> playerMap = redisTemplate.opsForHash().entries(playersKey);
        Set<Object> playerIdSet = playerMap.keySet();

        if (isOwner) {
            if (!playerIdSet.isEmpty()) {
                Long newOwnerId = Long.valueOf(playerIdSet.iterator().next().toString());
                redisTemplate.opsForHash().put(roomKey, Room.OWNER_ID, newOwnerId);
            } else {
                redisTemplate.delete(roomKey);
                redisTemplate.opsForZSet().remove("room:list", roomId);
                redisTemplate.delete(playersKey);
            }
        } else { // 非房主，检查是否没人了
            if (playerIdSet.isEmpty()) {
                redisTemplate.delete(roomKey);
                redisTemplate.opsForZSet().remove("room:list", roomId);
                redisTemplate.delete(playersKey);
            }
        }
        return true;
    }

    // todo 控制一个用户只能创建一个房间
    @Override
    public Room createRoom(RoomAddRequest roomAddRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Long roomId = redisTemplate.opsForValue().increment("room:id:incr");
        String name = roomAddRequest.getName();
        int maxPlayers = roomAddRequest.getMaxPlayers();
        RoomMember roomMember = new RoomMember();
        roomMember.setUserId(loginUser.getId());
        roomMember.setReady(false);

        Room room = new Room();
        room.setId(roomId);
        room.setOwnerId(loginUser.getId());
        room.setName(name);
        room.setMaxPlayers(maxPlayers);
        room.setCreatedTime(new Date().getTime());
        room.setRoomMembers(Collections.singletonList(roomMember));

        String roomKey = "room:" + roomId;
        Map<String, Object> roomMap = convertRoomToMap(room);
        redisTemplate.opsForHash().putAll(roomKey, roomMap);

        String playersKey = "room:" + roomId + ":members";
        redisTemplate.opsForHash().put(playersKey, String.valueOf(loginUser.getId()), JSONUtil.toJsonStr(roomMember));

        if (roomId != null) {
            redisTemplate.opsForZSet().add("room:list", roomId, room.getCreatedTime());
        }

        return room;
    }

    @Override
    public Room joinRoom(Long roomId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断房间是否存在
        String roomKey = "room:" + roomId;
        Boolean hasRoom = redisTemplate.hasKey(roomKey);
        ThrowUtils.throwIf(Boolean.FALSE.equals(hasRoom), ErrorCode.NOT_FOUND_ERROR, "房间不存在");

        // 判断房间是否已满
        Integer maxPlayers = (Integer) redisTemplate.opsForHash().get(roomKey, Room.MAX_PLAYERS);
        String playersKey = "room:" + roomId + ":members";
        Long currentPlayers = redisTemplate.opsForHash().size(playersKey); // 用 hash size
        ThrowUtils.throwIf(maxPlayers != null && currentPlayers >= maxPlayers,
                ErrorCode.PARAMS_ERROR, "房间已满");

        RoomMember roomMember = new RoomMember();
        roomMember.setUserId(loginUser.getId());
        roomMember.setReady(false);
        redisTemplate.opsForHash().put(playersKey, String.valueOf(loginUser.getId()), JSONUtil.toJsonStr(roomMember));

        Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);

        return convertMapToRoom(roomMap);
    }

    private Room convertMapToRoom(Map<Object, Object> map) {
        Room room = new Room();
        if (map == null) return room;
        if (map.containsKey(Room.ID)) room.setId(Long.valueOf(String.valueOf(map.get(Room.ID))));
        if (map.containsKey(Room.OWNER_ID)) room.setOwnerId(Long.valueOf(String.valueOf(map.get(Room.OWNER_ID))));
        if (map.containsKey(Room.NAME)) room.setName(String.valueOf(map.get(Room.NAME)));
        if (map.containsKey(Room.MAX_PLAYERS))
            room.setMaxPlayers(Integer.parseInt(String.valueOf(map.get(Room.MAX_PLAYERS))));
        if (map.containsKey(Room.CREATED_TIME))
            room.setCreatedTime(Long.valueOf(String.valueOf(map.get(Room.CREATED_TIME))));

        String playersKey = "room:" + map.get(Room.ID) + ":members";
        Map<Object, Object> playerMap = redisTemplate.opsForHash().entries(playersKey);
        List<RoomMember> roomMembers = new ArrayList<>();
        if (!playerMap.isEmpty()) {
            roomMembers = playerMap.values().stream()
                    .map(obj -> JSONUtil.toBean(String.valueOf(obj), RoomMember.class))
                    .collect(Collectors.toList());
        }
        room.setRoomMembers(roomMembers);

        return room;
    }


}
