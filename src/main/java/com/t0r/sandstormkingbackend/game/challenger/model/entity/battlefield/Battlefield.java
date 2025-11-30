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

@Data
@Slf4j
@Accessors(chain = true)
public class Battlefield {

    String name;

    Map<Long, HalfBattlefield> halfBattlefieldMap;

    List<Battle> battleList;

    public Battlefield setPlayerToBattlefield(String currentRound, Map<Long, ChallengerPlayer> challengerPlayers) {
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            halfBattlefieldMap.put(challengerPlayer.getUserId(), new HalfBattlefield());
        }
        return this; // 链式调用
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     * @return 先开始出牌的玩家 ID
     */
    public Long startBattle(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("开始战斗，战场 {}", name);

        for (Map.Entry<Long, HalfBattlefield> entry : halfBattlefieldMap.entrySet()) {
            Long userId = entry.getKey();
            LinkedList<CardInstance> handCardInstances = challengerPlayers.get(userId).getHandCardInstances();

            LinkedList<CardInstance> ShuffledHandCardInstances = MyListUtil.shuffleLinkedList(handCardInstances);

            HalfBattlefield halfBattlefield = entry.getValue();
            halfBattlefield.getHandZone().addAll(ShuffledHandCardInstances); // 手牌的副本
        }

        return decideStartPlayer(challengerPlayers);
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     * @return 先开始出牌的玩家 ID
     */
    public Long decideStartPlayer(Map<Long, ChallengerPlayer> challengerPlayers) {
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
            return Math.random() < 0.5 ? first.getKey() : second.getKey();
        } else {
            return first.getKey();
        }
    }


}
