package com.t0r.sandstormkingbackend.model.dto.rSocket;

import com.t0r.sandstormkingbackend.model.dto.game.WSMessage;
import lombok.Data;

@Data
public class ForwardedMessageRequest {

    private Long userId;

    private Long roomId;

    private WSMessage wsMessage;

}
