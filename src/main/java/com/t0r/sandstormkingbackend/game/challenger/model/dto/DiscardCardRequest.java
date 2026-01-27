
package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class DiscardCardRequest {
    private Long roomId;
    private Long userId;
    private Set<Integer> cardInstanceIds;
}