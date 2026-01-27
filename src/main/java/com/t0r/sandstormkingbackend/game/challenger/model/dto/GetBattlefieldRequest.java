
package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

@Data
public class GetBattlefieldRequest {
    private Long roomId;
    private String battlefieldName;
}