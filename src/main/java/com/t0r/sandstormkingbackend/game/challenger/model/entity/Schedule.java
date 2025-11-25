package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class Schedule {

    private List<TournamentSlot> slots;

}

@Data
class TournamentSlot {

    private Integer round;

    private String battlefield;

    private String seat;
}
