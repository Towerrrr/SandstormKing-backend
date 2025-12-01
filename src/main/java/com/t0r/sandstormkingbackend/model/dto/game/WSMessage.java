package com.t0r.sandstormkingbackend.model.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WSMessage {

    private String type;

    private String description;

    private GameMessage gameMessage;

}

