package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CupInstanceDeck {

    private String round;

    // 用于 JSON 加载
    private List<Integer> fanCount;

    private LinkedList<CupInstance> cupInstanceList;

    public void parseCupInstance() {
        cupInstanceList = fanCount.stream()
                .map(fanCount -> new CupInstance(round, fanCount))
                .collect(Collectors.toCollection(LinkedList::new));
    }

}
