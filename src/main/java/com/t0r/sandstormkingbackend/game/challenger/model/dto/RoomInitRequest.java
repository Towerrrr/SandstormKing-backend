package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoomInitRequest {

    private Long roomId;

    private Integer playerCount;

    private String version;

    private List<Long> userIds;

}
