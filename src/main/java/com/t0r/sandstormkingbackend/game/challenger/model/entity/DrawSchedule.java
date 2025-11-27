package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class DrawSchedule {

    private String round;

    private List<Option> options;

}
