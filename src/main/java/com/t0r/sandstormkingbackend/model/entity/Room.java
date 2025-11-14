package com.t0r.sandstormkingbackend.model.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Room {

    public final static String ID = "id";
    public final static String OWNER_ID = "ownerId";
    public final static String NAME = "name";
    public final static String MAX_PLAYERS = "maxPlayers";
    public final static String CREATED_TIME = "createdTime";

    private Long id;

    private Long ownerId;

    private String name;

    // WAITING, PLAYING, ENDED
    // private RoomStatus status;

    private List<Long> playerIds;

    private int maxPlayers;

    private Long createdTime;

    // todo 等无人连接之后过五分钟过期
    // private Long expireTime;

}
