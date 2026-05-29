package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;

import java.util.LinkedList;

@Data
public class Battle {

    private CardInstance defender;
    Power defenderPower = new Power(0);

    private LinkedList<CardInstance> attacker = new LinkedList<>();
    Integer attackerPower = 0;

    public boolean isAttackerWeakerThanDefender() {
        return attackerPower < defenderPower.getValue();
    }

    public boolean isFirstAttack() {
        return defenderPower.getValue() == 0;
    }

    public void addAttackerCard(CardInstance cardInstance) {
        attacker.add(cardInstance);
    }

    public void addAttackerPower(int power) {
        this.attackerPower += power;
    }

}
