package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Data
public class BattleLog {

    // TODO 前端需不需要自己记录一个 ID？
    // 每个元素：战场每有移动的一个快照
    LinkedList<HalfBattleLog> battleLogList = new LinkedList<>();

    public void updateDefenderRestZone(Long userId, Map<String, LinkedList<CardInstance>> restZone) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(String.valueOf(userId));
        halfBattleLog.setChangeRestZone(true);
        halfBattleLog.setChangeBattleZone(true);
        halfBattleLog.setChangePower(true);
        halfBattleLog.setRestZone(new LinkedHashMap<>(restZone));
        halfBattleLog.setBattleZone(new LinkedList<>());
        halfBattleLog.setPower(0);

        battleLogList.add(halfBattleLog);
    }

    public void updateDefenderPower(Long userId, int defenderPower) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(String.valueOf(userId));
        halfBattleLog.setChangePower(true);
        halfBattleLog.setPower(defenderPower);

        battleLogList.add(halfBattleLog);
    }

    public void updateAttackerBattleZone(Long userId, LinkedList<CardInstance> battleZone, int attackerPower) {
        HalfBattleLog halfBattleLog = new HalfBattleLog();
        halfBattleLog.setUserId(String.valueOf(userId));
        halfBattleLog.setChangeBattleZone(true);
        halfBattleLog.setChangePower(true);
        halfBattleLog.setBattleZone(new LinkedList<>(battleZone));
        halfBattleLog.setPower(attackerPower);

        battleLogList.add(halfBattleLog);
    }

    // TODO 消耗牌堆更新

}

@Data
class HalfBattleLog {

    private String userId;
    private boolean isChangeRestZone = false;
    private boolean isChangeBattleZone = false;
    private boolean isChangeConsumedZone = false;
    private boolean isChangePower = false;
    private Map<String, LinkedList<CardInstance>> restZone;
    private LinkedList<CardInstance> battleZone;
    private LinkedList<CardInstance> consumedZone;
    private int power;

}
