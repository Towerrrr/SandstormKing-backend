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
import java.util.ArrayList;
import java.util.Date;

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

        return room;
    }
}
