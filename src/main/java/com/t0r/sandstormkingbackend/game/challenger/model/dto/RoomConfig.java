package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

@Data
public class RoomConfig {

    private Long roomId;

    private Integer playerCount;

    private String version;

}
