package com.t0r.sandstormkingbackend.service.impl;

import com.t0r.sandstormkingbackend.common.PageRequest;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
        String playersKey = "room:" + roomId + ":players";

        Boolean hasRoom = redisTemplate.hasKey(roomKey);
        ThrowUtils.throwIf(Boolean.FALSE.equals(hasRoom), ErrorCode.NOT_FOUND_ERROR, "房间不存在");

        Long removedCount = redisTemplate.opsForSet().remove(playersKey, loginUser.getId());
        if (removedCount == null || removedCount == 0) {
            // 用户本来就不在房间
            return false;
        }

        // 检查用户是否为房主
        Object ownerIdObj = redisTemplate.opsForHash().get(roomKey, Room.OWNER_ID);
        Long ownerId = ownerIdObj != null ? Long.valueOf(ownerIdObj.toString()) : null;

        boolean isOwner = loginUser.getId().equals(ownerId);

        // 获取退出后的所有成员
        Set<Object> playerIdSet = redisTemplate.opsForSet().members(playersKey);

        if (isOwner) {
            if (playerIdSet != null && !playerIdSet.isEmpty()) {
                Long newOwnerId = Long.valueOf(playerIdSet.iterator().next().toString());
                redisTemplate.opsForHash().put(roomKey, Room.OWNER_ID, newOwnerId);
            } else {
                redisTemplate.delete(roomKey);
                redisTemplate.opsForZSet().remove("room:list", roomId);
                redisTemplate.delete(playersKey);
            }
        } else { // 非房主，检查是否没人了
            if (playerIdSet == null || playerIdSet.isEmpty()) {
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

        Room room = new Room();
        room.setId(roomId);
        room.setOwnerId(loginUser.getId());
        room.setName(name);
        room.setMaxPlayers(maxPlayers);
        room.setCreatedTime(new Date().getTime());
        room.setPlayerIds(Collections.singletonList(loginUser.getId()));

        String roomKey = "room:" + roomId;
        Map<String, Object> roomMap = convertRoomToMap(room);
        redisTemplate.opsForHash().putAll(roomKey, roomMap);

        String playersKey = "room:" + roomId + ":players";
        redisTemplate.opsForSet().add(playersKey, loginUser.getId());

        redisTemplate.opsForZSet().add("room:list", roomId, room.getCreatedTime());

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
        String playersKey = "room:" + roomId + ":players";
        Long currentPlayers = redisTemplate.opsForSet().size(playersKey);
        ThrowUtils.throwIf(currentPlayers != null && maxPlayers != null && currentPlayers >= maxPlayers,
                ErrorCode.PARAMS_ERROR, "房间已满");

        redisTemplate.opsForSet().add(playersKey, loginUser.getId());

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
        if (map.containsKey(Room.CREATED_TIME)) room.setCreatedTime(Long.valueOf(String.valueOf(map.get(Room.CREATED_TIME))));

        String playersKey = "room:" + map.get(Room.ID) + ":players";
        Set<Object> playerIdSet = redisTemplate.opsForSet().members(playersKey);
        if (playerIdSet != null) {
            List<Long> playerIdList = playerIdSet.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            room.setPlayerIds(playerIdList);
        }
        return room;
    }

    private Map<String, Object> convertRoomToMap(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put(Room.ID, room.getId());
        map.put(Room.OWNER_ID, room.getOwnerId());
        map.put(Room.NAME, room.getName());
        map.put(Room.MAX_PLAYERS, room.getMaxPlayers());
        map.put(Room.CREATED_TIME, room.getCreatedTime());
        return map;
    }

}
