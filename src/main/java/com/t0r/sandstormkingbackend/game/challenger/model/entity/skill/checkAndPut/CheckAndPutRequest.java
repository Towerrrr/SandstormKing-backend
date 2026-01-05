package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;

@RequiredArgsConstructor
public class CheckAndPutRequest {

    private final LinkedList<CardInstance> waitingForCheck;

    private final WhereToPutEnum[] whereToPutEnums;

}
