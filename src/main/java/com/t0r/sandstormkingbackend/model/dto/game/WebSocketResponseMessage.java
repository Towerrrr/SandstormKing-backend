package com.t0r.sandstormkingbackend.model.dto.game;

import com.t0r.sandstormkingbackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponseMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 玩家执行的操作
     */
    private String playerAction;

    /**
     * 用户信息
     */
    private UserVO user;
}

