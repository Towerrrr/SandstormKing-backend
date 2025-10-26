package com.t0r.sandstormkingbackend.model.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketRequestMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息体
     * todo 后续不一定这么设计
     */
    private String data;
}

