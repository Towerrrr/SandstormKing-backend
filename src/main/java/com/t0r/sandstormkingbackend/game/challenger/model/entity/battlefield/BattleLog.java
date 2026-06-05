package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Data
public class BattleLog {

    // TODO 前端需不需要自己记录一个 ID？
    // 每个元素：战场每有移动的一个快照
    LinkedList<HalfBattleLog> battleLogMap = new LinkedList<>();

    public void updateDefenderRestZone(Long userId, Map<String, LinkedList<CardInstance>> restZone) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(userId);
        halfBattleLog.setChangeRestZone(true);
        halfBattleLog.setRestZone(restZone);

        battleLogMap.add(halfBattleLog);
    }

    public void updateDefenderPower(Long userId, int defenderPower) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(userId);
        halfBattleLog.setChangeDefenderPower(true);
        halfBattleLog.setDefenderPower(defenderPower);

        battleLogMap.add(halfBattleLog);
    }

    public void updateAttackerBattleZone(Long userId, LinkedList<CardInstance> battleZone, int attackerPower) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(userId);
        halfBattleLog.setChangeBattleZone(true);
        halfBattleLog.setBattleZone(battleZone);
        halfBattleLog.setAttackerPower(attackerPower);

        battleLogMap.add(halfBattleLog);
    }

    // TODO 消耗牌堆更新

}

@Data
class HalfBattleLog {

    private Long userId;
    private boolean isChangeRestZone = false;
    private boolean isChangeBattleZone = false;
    private boolean isChangeConsumedZone = false;
    private boolean isChangeDefenderPower = false;
    private Map<String, LinkedList<CardInstance>> restZone;
    private LinkedList<CardInstance> battleZone;
    private LinkedList<CardInstance> consumedZone;

    private int attackerPower;
    private int defenderPower;

}
