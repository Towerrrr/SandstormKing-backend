package com.t0r.sandstormkingbackend.service.impl;

import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.service.RoomService;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisTemplate<String, Room> redisTemplate;

    @Override
    public Room addRoom(RoomAddRequest roomAddRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        String name = roomAddRequest.getName();
        int maxPlayers = roomAddRequest.getMaxPlayers();

        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setOwnerId(loginUser.getId());
        room.setName(name);
        room.setMaxPlayers(maxPlayers);
        room.setCreatedTime(new Date());

        // todo 存到 Redis

        return room;
    }
}
