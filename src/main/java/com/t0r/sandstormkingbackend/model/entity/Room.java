package com.t0r.sandstormkingbackend.model.entity;

import java.util.List;

public class Room {

    private Long id;

    private Long ownerId;

    private String name;

    // WAITING, PLAYING, ENDED
    // private RoomStatus status;

    private List<String> playerIds;

    private int maxPlayers;

    private Long createdTime;

    private Long expireTime;

}
