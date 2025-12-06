package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class BuildDeckRequest {

    private Integer optionId;

    private Set<Integer> selectedCardInstanceIds;

}
