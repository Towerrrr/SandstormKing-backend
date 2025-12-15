package com.t0r.sandstormkingbackend.game.challenger.model.entity.buff;

import lombok.Data;

@Data
public class BuffConfigParam {

    /**
     * 仅 “控制旗帜” / “进攻时”
     */
    private String timeRange = null;

    private Integer basePower = null;

    private String group = null;

    private String cardName = null;

    private Integer extraPower = null;

}
