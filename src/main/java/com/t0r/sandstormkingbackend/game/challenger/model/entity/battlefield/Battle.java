package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;

import java.util.LinkedList;

@Data
public class Battle {

    private CardInstance defender;

    private LinkedList<CardInstance> attacker;

}
