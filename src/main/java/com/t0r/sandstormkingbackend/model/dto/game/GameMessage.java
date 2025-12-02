package com.t0r.sandstormkingbackend.model.dto.game;

import lombok.Data;

import java.util.List;

@Data
public class GameMessage {

    private String type;

    private String description;

    // 这条消息目标发送的用户id
    private List<Long> userIds;

    private String body;

}
