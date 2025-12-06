package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class InitGameRequest {

    private Long roomId;

    private Integer playerCount;

    private String version;

    private Set<Long> userIds;

}
