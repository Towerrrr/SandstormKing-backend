package com.t0r.sandstormkingbackend.model.dto.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoomAddRequest implements Serializable {

    private static final long serialVersionUID = -2802323623546539956L;

    private String name;

    private int maxPlayers;

}
