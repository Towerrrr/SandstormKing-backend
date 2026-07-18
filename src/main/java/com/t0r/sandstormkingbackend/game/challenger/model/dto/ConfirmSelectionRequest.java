package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ConfirmSelectionRequest {

    private Long roomId;
    private Long userId;
    private Integer optionId;
    private Set<Integer> selectedCardInstanceIds;

}
