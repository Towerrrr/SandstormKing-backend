package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;


import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Getter;

import java.util.LinkedList;

@Getter
public class CheckAndPutResponse {

    Put[] puts;

}

@Getter
class Put {

    WhereToPutEnum whereToPutEnum;

    LinkedList<CardInstance> cardInstances;

}