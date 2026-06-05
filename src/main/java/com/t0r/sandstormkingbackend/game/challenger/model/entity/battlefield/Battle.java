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

    /**
     * 等待被移入休息区中
     */
    private LinkedList<CardInstance> awaitingRest = new LinkedList<>();


    public boolean isAttackerWeakerThanDefender() {
        return attackerPower < defenderPower.getValue();
    }

    public boolean isFirstAttack() {
        // TODO 改成第一次攻击的标记会不会更优雅？？
        return (defenderPower.getValue() == 0) && (attackerPower == 0);
    }

    public void addAttackerCard(CardInstance cardInstance) {
        attacker.add(cardInstance);
    }

    public void addAttackerPower(int power) {
        this.attackerPower += power;
    }

    public boolean isWaitingToRest() {
        return defender != null;
    }

    public CardInstance swapAttackAndDefense(Power tempAttackerPower) {
        // 进攻方最后一张牌做防守方
        this.defender = attacker.removeLast();
        this.defenderPower.setValue(tempAttackerPower.getValue());
        this.attackerPower = 0;
        this.awaitingRest = this.attacker;
        this.attacker = new LinkedList<>();

        return this.defender;
    }

}
