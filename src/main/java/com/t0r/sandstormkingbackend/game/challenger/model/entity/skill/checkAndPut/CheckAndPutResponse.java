package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;


import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Getter;

import java.util.LinkedList;

@Getter
public class CheckAndPutResponse {

    private String userId;

    /**
     * 在查看所有卡牌并放置到最后的时候用
     */
    Integer cardInstanceId = null;

    Put[] puts;

}

@Getter
class Put {

    WhereToPutEnum whereToPutEnum;

    LinkedList<CardInstance> cardInstances;

}