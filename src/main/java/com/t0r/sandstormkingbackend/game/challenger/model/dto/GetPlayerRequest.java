
package com.t0r.sandstormkingbackend.game.challenger.model.dto;

import lombok.Data;

@Data
public class GetPlayerRequest {
    private Long roomId;
    private Long userId;
}