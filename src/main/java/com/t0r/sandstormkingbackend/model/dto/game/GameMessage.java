package com.t0r.sandstormkingbackend.model.dto.game;

import lombok.Data;

@Data
public class GameMessage {

    private String type;

    private String description;

    private String request;

}
