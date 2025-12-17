package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.Util.MyListUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CupInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.Buff;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.PhaseEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.StartWayEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TimeRangeEnum;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;
import static com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.HalfBattlefield.MAX_REST_ZONE_SIZE;

@Data
@Slf4j
@Accessors(chain = true)
public class Battlefield {

    String name;

    String currentPhase;

    Map<Long, HalfBattlefield> halfBattlefieldMap = new HashMap<>();

    Long startPlayerId;
    Long elsePlayerId;
    String startWay;

    LinkedList<Battle> battleList = new LinkedList<>();

    boolean isEnd = false;
    Long winnerId;

    public Battlefield(String name, String currentRound, Map<Long, ChallengerPlayer> challengerPlayers) {
        this.name = name;
        this.currentPhase = PhaseEnum.BUILD.getValue();
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            if (playerBattlefield.equals(this.name)) {
                halfBattlefieldMap.put(challengerPlayer.getUserId(), new HalfBattlefield());
            }
        }
    }

    public Long readyBattle(Long userId) {
        halfBattlefieldMap.get(userId).setReady(true);

        for (Long opponentId : halfBattlefieldMap.keySet()) {
            if (!Objects.equals(opponentId, userId)) return opponentId;
        }
        return null;
    }

    public boolean checkAllReady() {
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

        this.currentPhase = PhaseEnum.BATTLE.getValue();

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
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Map.Entry<Long, Integer> first = sortedEntries.get(0);
        Map.Entry<Long, Integer> second = sortedEntries.get(1);

        // 如果两个玩家的奖杯回合数相同，则随机选择一个玩家
        if (first.getValue().equals(second.getValue())) {
            log.info("两个玩家的奖杯回合数相同，随机选择一个玩家");
            this.startWay = StartWayEnum.RANDOM.getValue();
            if (Math.random() < 0.5) {
                startPlayerId = first.getKey();
                elsePlayerId = second.getKey();
            } else {
                startPlayerId = second.getKey();
                elsePlayerId = first.getKey();
            }
        } else {
            this.startWay = StartWayEnum.NORMAL.getValue();
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
        List<LinkedList<CardInstance>> handZones = Arrays.asList(
                halfBattlefields[0].getHandZone(),
                halfBattlefields[1].getHandZone()
        );
        List<List<Consumer<BuffCallParam>>> restBuffsArray = Arrays.asList(
                halfBattlefields[0].getRestBuffs(),
                halfBattlefields[1].getRestBuffs()
        );
        List<Consumer<BuffCallParam>> nextBuffs = Arrays.asList(
                halfBattlefields[0].getNextBuff(),
                halfBattlefields[1].getNextBuff()
        );

        int attackerIdx = 0;
        int defenderIdx = 1;
        Power defenderPower = new Power(0);
        Integer attackerPower;

        // TODO 判断有人战斗开始直接弃光手牌？

        // 最开始进攻方先放一张牌
        Battle battle = new Battle();
        CardInstance attackerCard = handZones.get(attackerIdx).removeFirst();
        battle.getAttacker().add(attackerCard);
        attackerPower = cardMap.get(attackerCard.getCardId()).getBasePower();

        // 进行到最后可能先没牌的玩家是赢的，两个人都没牌的情况下可能还要进一次循环
        while (true) {
            if (battle.getDefender() != null) { // 跳过第一次攻击
                // 给防守方上 休息区 BUFF
                BuffCallParam buffCallParam =
                        new BuffCallParam(
                                TimeRangeEnum.CONTROL_FLAG.getValue(),
                                defenderPower, cardMap.get(battle.getDefender().getCardId()));
                for (Consumer<BuffCallParam> restBuff : restBuffsArray.get(defenderIdx)) {
                    restBuff.accept(buffCallParam);
                }
            }

            // 进攻方出牌，直到攻击力 >= 防守力 或手牌用完
            while (attackerPower < Objects.requireNonNull(defenderPower).getValue() &&
                    !handZones.get(attackerIdx).isEmpty()) {
                attackerCard = handZones.get(attackerIdx).removeFirst();
                Power tempAttackerPower = new Power(cardMap.get(attackerCard.getCardId()).getBasePower());

                // 给进攻方上 休息区 BUFF
                BuffCallParam buffCallParam =
                        new BuffCallParam(
                                TimeRangeEnum.ATTACK.getValue(),
                                tempAttackerPower, cardMap.get(attackerCard.getCardId()));
                for (Consumer<BuffCallParam> restBuff : restBuffsArray.get(attackerIdx)) {
                    restBuff.accept(buffCallParam);
                }
                nextBuffs.get(attackerIdx).accept(buffCallParam);

                // TODO “下一张卡” BUFF 技能，要结合卡的 timeRange

                attackerPower += tempAttackerPower.getValue();
                battle.getAttacker().add(attackerCard);
            }
            battleList.add(battle);
            // 进攻方手牌耗尽攻击力依旧不足
            if (attackerPower < defenderPower.getValue()) {
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
                    Map<String, List<CardInstance>> restZone = halfBattlefields[defenderIdx].getRestZone();
                    Card card = cardMap.get(cardInstance.getId());
                    if (card.getBuffType().equals(BuffTypeEnum.REST.getValue())) { // "在休息区" 技能
                        restBuffsArray.get(defenderIdx).add(new Buff(card));
                    }

                    restZone.putIfAbsent(card.getName(), new ArrayList<>());
                    restZone.get(card.getName()).add(cardInstance);
                    if (restZone.size() > MAX_REST_ZONE_SIZE) {
                        winnerId = whoIsAttackerOrDefender(attackerIdx);
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
                defenderPower.setValue(cardMap.get(lastAttacker.getCardId()).getBasePower());
                attackerPower = 0;

                int temp = defenderIdx;
                defenderIdx = attackerIdx;
                attackerIdx = temp;
            }
        }
    }

    private Long whoIsAttackerOrDefender(int idx) {
        return idx == 0 ? startPlayerId : elsePlayerId;
    }
}
