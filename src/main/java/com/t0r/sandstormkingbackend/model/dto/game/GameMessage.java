package com.t0r.sandstormkingbackend.model.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class GameMessage {

    private String type;

    private String description;

    // 这条消息目标发送的用户id
    private Set<Long> userIds = new HashSet<>();

    private String body;

}
