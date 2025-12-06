package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.Util.MyListUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CupInstance;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.HalfBattlefield.MAX_REST_ZONE_SIZE;

@Data
@Slf4j
@Accessors(chain = true)
public class Battlefield {

    String name;

    Map<Long, HalfBattlefield> halfBattlefieldMap = new HashMap<>();

    Long startPlayerId;
    Long elsePlayerId;

    LinkedList<Battle> battleList = new LinkedList<>();

    Long winnerId;

    public Battlefield(String name, String currentRound, Map<Long, ChallengerPlayer> challengerPlayers) {
        this.name = name;
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            halfBattlefieldMap.put(challengerPlayer.getUserId(), new HalfBattlefield());
        }
    }

    public Long readyBattle(Long userId, Map<Long, ChallengerPlayer> challengerPlayers) {
        halfBattlefieldMap.get(userId).setReady(true);

        boolean allReady = true;
        for (HalfBattlefield halfBattlefield : halfBattlefieldMap.values()) {
            if (!halfBattlefield.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady) {
            new Thread(() -> {
                startBattle(challengerPlayers);
                calculateBattle();
            }).start();
        }

        for (Long opponentId : halfBattlefieldMap.keySet()) {
            if (!Objects.equals(opponentId, userId)) return opponentId;
        }
        return null;
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     */
    public void startBattle(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("开始战斗，战场 {}", name);

        for (Map.Entry<Long, HalfBattlefield> entry : halfBattlefieldMap.entrySet()) {
            Long userId = entry.getKey();
            LinkedList<CardInstance> handCardInstances = challengerPlayers.get(userId).getHandCardInstances();

            LinkedList<CardInstance> ShuffledHandCardInstances = MyListUtil.shuffleLinkedList(handCardInstances);

            HalfBattlefield halfBattlefield = entry.getValue();
            halfBattlefield.getHandZone().addAll(ShuffledHandCardInstances); // 手牌的副本
        }

        decideStartPlayer(challengerPlayers);
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     */
    public void decideStartPlayer(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("决定先开始出牌的玩家，战场 {}", name);

        // 用户 ID -> 拥有奖杯的回合数的和
        Map<Long, Integer> cupRoundSumMap = new HashMap<>();

        for (Long userId : halfBattlefieldMap.keySet()) {
            List<CupInstance> cupInstances = challengerPlayers.get(userId).getCupInstances();
            int cupRoundSum = 0;
            for (CupInstance cupInstance : cupInstances) {
                cupRoundSum += Integer.parseInt(cupInstance.getRound());
            }
            cupRoundSumMap.put(userId, cupRoundSum);
        }

        // TODO 这段不够优雅
        // 倒序排放
        List<Map.Entry<Long, Integer>> sortedEntries = cupRoundSumMap.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Map.Entry<Long, Integer> first = sortedEntries.get(0);
        Map.Entry<Long, Integer> second = sortedEntries.get(1);

        // 如果两个玩家的奖杯回合数相同，则随机选择一个玩家
        if (first.getValue().equals(second.getValue())) {
            // TODO 随机选择时告知前端
            log.info("两个玩家的奖杯回合数相同，随机选择一个玩家");
            if (Math.random() < 0.5) {
                startPlayerId = first.getKey();
                elsePlayerId = second.getKey();
            } else {
                startPlayerId = second.getKey();
                elsePlayerId = first.getKey();
            }
        } else {
            startPlayerId = first.getKey();
            elsePlayerId = second.getKey();
        }
    }

    public void calculateBattle() {
        HalfBattlefield[] halfBattlefields = new HalfBattlefield[]{
                halfBattlefieldMap.get(startPlayerId),
                halfBattlefieldMap.get(elsePlayerId)
        };
        // 0: 当前防守方, 1: 当前进攻方
        LinkedList<CardInstance>[] handZones = new LinkedList[]{
                halfBattlefields[0].getHandZone(),
                halfBattlefields[1].getHandZone()
        };

        int defenderIdx = 0;
        int attackerIdx = 1;

        while (!handZones[defenderIdx].isEmpty() && !handZones[attackerIdx].isEmpty()) {
            Integer defenderPower = 0;
            Integer attackerPower = 0;

            Battle battle = new Battle();
            // 防守方出牌
            CardInstance defenderCard = handZones[defenderIdx].removeFirst();
            battle.setDefender(defenderCard);
            defenderPower = defenderCard.getCurrentPower();

            // 进攻方出牌，直到攻击力 >= 防守力 或手牌用完
            while (attackerPower < defenderPower && !handZones[attackerIdx].isEmpty()) {
                CardInstance attackerCard = handZones[attackerIdx].removeFirst();
                attackerPower += attackerCard.getCurrentPower();
                battle.getAttacker().add(attackerCard);
            }
            // 将上一轮所有进攻牌放入休息区
            if (!battleList.isEmpty()) { // 第一轮不用
                LinkedList<CardInstance> attacker = battleList.getLast().getAttacker();
                for (CardInstance cardInstance : attacker) {
                    Map<Integer, List<CardInstance>> restZone = halfBattlefields[defenderIdx].getRestZone();
                    restZone.putIfAbsent(cardInstance.getCardId(), new ArrayList<>());
                    restZone.get(cardInstance.getCardId()).add(cardInstance);
                    if (restZone.size() > MAX_REST_ZONE_SIZE) {
                        winnerId = attackerIdx == 0 ? startPlayerId : elsePlayerId;
                        battleList.add(battle);
                        log.info("战斗结束，防守方休息区溢出，胜利者为 {}", winnerId);
                        return;
                    }
                }
            }

            battleList.add(battle);

            // 交换攻守（下回合由上一轮进攻方最后一张牌做防守方）
            if (!battle.getAttacker().isEmpty()) {
                // TODO 后续加入技能可能要改，这里将上一轮进攻的最后一张牌放回手牌
                CardInstance lastAttacker = battle.getAttacker().getLast();
                handZones[defenderIdx].addFirst(lastAttacker); // 加回防守方手牌最前面
            }

            int temp = defenderIdx;
            defenderIdx = attackerIdx;
            attackerIdx = temp;
        }

        if (handZones[0].isEmpty()) {
            winnerId = startPlayerId;
        } else {
            winnerId = elsePlayerId;
        }
        log.info("战斗结束，进攻方无多余手牌，胜利者为 {}", winnerId);
    }


}
