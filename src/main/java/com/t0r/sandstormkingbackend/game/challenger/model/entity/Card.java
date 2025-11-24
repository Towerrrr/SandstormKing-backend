package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

@Data
public class Card {

    private int id;

    private String name;

    private int basePower;

    private String race;

    private String description;

}
