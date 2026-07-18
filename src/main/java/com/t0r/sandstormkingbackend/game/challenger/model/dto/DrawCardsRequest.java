package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

@Data
public class DrawCardsRequest {

    private Long roomId;
    private Long userId;
    private Integer optionId;

}
