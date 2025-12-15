package com.t0r.sandstormkingbackend.game.challenger.model.entity.buff;

import lombok.Data;

@Data
public class BuffConfigParam {

    /**
     * 仅 “控制旗帜” / “进攻时”
     */
    private String timeRange;

    private Integer basePower;

    private String group;

    private String cardName;

    private Integer extraPower;

}
