package com.t0r.sandstormkingbackend.service.impl;

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

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Room addRoom(RoomAddRequest roomAddRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Long roomId = redisTemplate.opsForValue().increment("room:id:incr");
        String name = roomAddRequest.getName();
        int maxPlayers = roomAddRequest.getMaxPlayers();

        Room room = new Room();
        room.setId(roomId);
        room.setOwnerId(loginUser.getId());
        room.setName(name);
        room.setMaxPlayers(maxPlayers);
        room.setCreatedTime(new Date());
        // todo 返回前端的vo和在Redis中的一致性怎么保证
        room.setPlayerIds(new ArrayList<>());

        String roomKey = "room:" + roomId;
        redisTemplate.opsForHash().put(roomKey, Room.ID, roomId);
        redisTemplate.opsForHash().put(roomKey, Room.OWNER_ID, loginUser.getId());
        redisTemplate.opsForHash().put(roomKey, Room.NAME, name);
        redisTemplate.opsForHash().put(roomKey, Room.MAX_PLAYERS, maxPlayers);
        redisTemplate.opsForHash().put(roomKey, Room.CREATED_TIME, room.getCreatedTime());

        String playersKey = "room:" + roomId + ":players";
        redisTemplate.opsForSet().add(playersKey, loginUser.getId());

        double score = room.getCreatedTime().getTime();
        redisTemplate.opsForZSet().add("room:list", roomId, score);

        return room;
    }

    @Override
    public Room joinRoom(String roomIdStr, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 1. 校验房间是否存在
        String roomKey = "room:" + roomIdStr;
        Boolean hasRoom = redisTemplate.hasKey(roomKey);
        ThrowUtils.throwIf(Boolean.FALSE.equals(hasRoom), ErrorCode.NOT_FOUND_ERROR);

        // 2. 校验房间人数未满
        Integer maxPlayers = (Integer) redisTemplate.opsForHash().get(roomKey, Room.MAX_PLAYERS);
        String playersKey = "room:" + roomIdStr + ":players";
        Long currentPlayers = redisTemplate.opsForSet().size(playersKey);
        ThrowUtils.throwIf(currentPlayers != null && maxPlayers != null && currentPlayers >= maxPlayers, ErrorCode.PARAMS_ERROR, "房间已满");

        // 3. 用户加入玩家集合
        redisTemplate.opsForSet().add(playersKey, loginUser.getId());

        // 4. 更新房间详情里的playerIds字段（可选，如果你要同步VO和Redis中的一致性）
        // 你可以读取所有玩家ID，然后更新Room对象（如果有需要）
        // 这里只是简单把用户加进集合

        // 5. 返回房间详情
        Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);
        Room room = convertMapToRoom(roomMap);

        // 从Redis拿出所有玩家ID，设置到Room对象
        Set<Object> playerIdSet = redisTemplate.opsForSet().members(playersKey);
        if (playerIdSet != null) {
            List<Long> playerIds = new ArrayList<>();
            for (Object idObj : playerIdSet) {
                playerIds.add(Long.valueOf(String.valueOf(idObj)));
            }
            room.setPlayerIds(playerIds);
        }

        return room;
    }

    // 可以写一个辅助方法将Hash转为Room对象
    private Room convertMapToRoom(Map<Object, Object> map) {
        Room room = new Room();
        if (map == null) return room;
        if (map.containsKey(Room.ID)) room.setId(Long.valueOf(String.valueOf(map.get(Room.ID))));
        if (map.containsKey(Room.OWNER_ID)) room.setOwnerId(Long.valueOf(String.valueOf(map.get(Room.OWNER_ID))));
        if (map.containsKey(Room.NAME)) room.setName(String.valueOf(map.get(Room.NAME)));
        if (map.containsKey(Room.MAX_PLAYERS)) room.setMaxPlayers(Integer.valueOf(String.valueOf(map.get(Room.MAX_PLAYERS))));
        if (map.containsKey(Room.CREATED_TIME)) room.setCreatedTime((Date) map.get(Room.CREATED_TIME));
        // 其它字段...
        return room;
    }

    public List<Room> listRooms(int page, int pageSize) {
        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<Object> roomIdSet = redisTemplate.opsForZSet().reverseRange("room:list", start, end);
        List<Room> roomList = new ArrayList<>();
        if (roomIdSet != null) {
            for (Object roomId : roomIdSet) {
                String roomKey = "room:" + roomId;
                Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);
                roomList.add(room);
            }
        }
        return roomList;
    }

}
