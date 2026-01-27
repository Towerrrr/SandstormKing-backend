
package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

@Data
public class ReadyBattleRequest {
    private Long roomId;
    private Long userId;
    private String battlefieldName;
}