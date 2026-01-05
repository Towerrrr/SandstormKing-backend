package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;

import lombok.Data;

@Data
public class CheckAndPutParam {

    WhereToCheckEnum whereToCheckEnum;

    Integer count = null;

    WhereToPutEnum[] whereToPutEnums;

}
