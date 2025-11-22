package com.t0r.sandstormkingbackend.model.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private List<RoomMember> roomMembers;

    private int maxPlayers;

    private Long createdTime;

    // todo 等无人连接之后过五分钟过期
    // private Long expireTime;

    public static Map<String, Object> convertRoomToMap(Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put(Room.ID, room.getId());
        map.put(Room.OWNER_ID, room.getOwnerId());
        map.put(Room.NAME, room.getName());
        map.put(Room.MAX_PLAYERS, room.getMaxPlayers());
        map.put(Room.CREATED_TIME, room.getCreatedTime());
        return map;
    }
}
