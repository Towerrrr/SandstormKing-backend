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

    boolean isEnd = false;
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

        for (Long opponentId : halfBattlefieldMap.keySet()) {
            if (!Objects.equals(opponentId, userId)) return opponentId;
        }
        return null;
    }

    public boolean checkAllReady(Map<Long, ChallengerPlayer> challengerPlayers) {
        boolean allReady = true;
        for (HalfBattlefield halfBattlefield : halfBattlefieldMap.values()) {
            if (!halfBattlefield.isReady()) {
                allReady = false;
                break;
            }
        }
        return allReady;
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
        Long[] playerIds = new Long[]{startPlayerId, elsePlayerId};
        HalfBattlefield[] halfBattlefields = new HalfBattlefield[]{
                halfBattlefieldMap.get(startPlayerId),
                halfBattlefieldMap.get(elsePlayerId)
        };
        // 0: 当前防守方, 1: 当前进攻方
        LinkedList<CardInstance>[] handZones = new LinkedList[]{
                halfBattlefields[0].getHandZone(),
                halfBattlefields[1].getHandZone()
        };

        int attackerIdx = 0;
        int defenderIdx = 1;

        Integer defenderPower = 0;
        Integer attackerPower = 0;

        // TODO 判断有人战斗开始直接弃光手牌？

        // 最开始进攻方先放一张牌
        Battle battle = new Battle();
        CardInstance attackerCard = handZones[attackerIdx].removeFirst();
        battle.getAttacker().add(attackerCard);
        attackerPower = attackerCard.getCurrentPower();

        // 进行到最后可能先没牌的玩家是赢的
        while (!handZones[defenderIdx].isEmpty() || !handZones[attackerIdx].isEmpty()) {
            // 进攻方出牌，直到攻击力 >= 防守力 或手牌用完
            while (attackerPower < defenderPower && !handZones[attackerIdx].isEmpty()) {
                attackerCard = handZones[attackerIdx].removeFirst();
                attackerPower += attackerCard.getCurrentPower();
                battle.getAttacker().add(attackerCard);
            }
            battleList.add(battle);
            // 进攻方手牌耗尽攻击力依旧不足
            if (attackerPower < defenderPower) {
                winnerId = defenderIdx == 0 ? startPlayerId : elsePlayerId;
                log.info("战斗结束，进攻方无多余手牌，胜利者为 {}", winnerId);
                break;
            }
            // 将上一轮的上一轮所有进攻牌放入休息区
            if (battleList.size() >= 2) {
                Iterator<Battle> descendingIterator = battleList.descendingIterator();
                descendingIterator.next();
                LinkedList<CardInstance> attacker = descendingIterator.next().getAttacker();

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

            // 交换攻守（下回合由上一轮进攻方最后一张牌做防守方）
            if (!battle.getAttacker().isEmpty()) {
                CardInstance lastAttacker = battle.getAttacker().getLast();
                battle = new Battle();
                battle.setDefender(lastAttacker);
                defenderPower = lastAttacker.getCurrentPower();
                attackerPower = 0;
            }

            int temp = defenderIdx;
            defenderIdx = attackerIdx;
            attackerIdx = temp;
        }
    }
}
