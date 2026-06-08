package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult.ConditionAndResult;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.selectAndMoveOrResult.SelectAndMoveOrResult;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.checkAndPut.CheckAndPut;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.*;
import com.t0r.sandstormkingbackend.game.challenger.model.event.EndBattleEvent;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

@Data
@Slf4j
@Accessors(chain = true)
public class Battlefield {

    Long roomId;
    String currentRound;
    String name;
    String currentPhase;

    Map<Long, BattleSeat> halfBattlefieldMap = new HashMap<>();
    Map<Long, ChallengerPlayer> playerMap = new HashMap<>();

    Long startPlayerId;
    Long elsePlayerId;
    String startWay;

    BattleLog battleLog = new BattleLog();

    // TODO 先设置成 0 吗
    WinnerId winnerId = new WinnerId(0);

    private final ApplicationEventPublisher eventPublisher;

    private BattleStateEnum currentState;

    // region 战斗重构变量

    // TODO 判断有人战斗开始直接弃光手牌？

    private BattleSeat attacker;
    private BattleSeat defender;

    Battle battle = new Battle();
    Power tempAttackerPower = new Power(0);

    // endregion

    public Battlefield(Long roomId, String name, String currentRound, Map<Long, ChallengerPlayer> challengerPlayers,
                       ApplicationEventPublisher eventPublisher,
                       Map<String, Deque<CardInstance>> mainDecks, Map<String, Deque<CardInstance>> discardDecks) {
        this.roomId = roomId;
        this.currentRound = currentRound;
        this.name = name;
        this.currentPhase = PhaseEnum.BUILD.getValue();
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            if (playerBattlefield.equals(this.name)) {
                halfBattlefieldMap.put(challengerPlayer.getUserId(),
                        new BattleSeat(challengerPlayer.getUserId(), mainDecks, discardDecks));
                playerMap.put(challengerPlayer.getUserId(), challengerPlayer);
            }
        }
        this.eventPublisher = eventPublisher;
    }

    public Mono<Void> advanceBattle() {
        return Mono.defer(() -> {
                    log.info("当前战斗状态: {}", currentState);

                    switch (currentState) {
                        case playCards:
                            PlayCards playCards =
                                    new PlayCards(currentRound, battle, attacker, defender, tempAttackerPower, playerMap, winnerId, battleLog);
                            this.currentState = playCards.castAttack();
                            break;
                        case checkAttackPower:
                            checkAttackPower();
                            break;
                        case moveAttackerToRestZone:
                            moveAttackerToRestZone();
                            break;
                        case swapAttackAndDefense:
                            swapAttackAndDefense();
                            break;
                        case endBattle:
                            endBattle();
                            break;
                        case checkAndPut:
                        case selectCard:
                            break;
                        default:
                            throw new RuntimeException("未知的战斗状态: " + currentState);
                    }

                    switch (currentState) {
                        case outBattle:
                            return Mono.empty();
//                        case selectCard:
//                            return SelectAndMoveOrResult
//                                    // TODO attackerCard 这里已经废弃
//                                    .apply(attackerCard, attacker, defender, eventPublisher, this, tempAttackerPower)
//                                    .then(Mono.defer(this::advanceBattle));
//                        case checkAndPut:
//                            return CheckAndPut.apply(attackerCard, attacker, eventPublisher, this)
//                                    .then(Mono.defer(this::advanceBattle));
                        default:
                            return Mono.defer(this::advanceBattle);
                    }
                })
                .doOnError(e -> log.error("战斗状态机异常", e));
    }

    public Long readyBattle(Long userId) {
        halfBattlefieldMap.get(userId).setReady(true);

        for (Long opponentId : halfBattlefieldMap.keySet()) {
            if (!Objects.equals(opponentId, userId))
                return opponentId;
        }
        return null;
    }

    public boolean checkAllReady() {
        return halfBattlefieldMap.values().stream().allMatch(BattleSeat::isReady);
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     */
    public void startBattle(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("开始战斗，战场 {}", name);
        this.currentPhase = PhaseEnum.BATTLE.getValue();

        for (BattleSeat battleSeat : halfBattlefieldMap.values()) {
            LinkedList<CardInstance> handCardInstances = challengerPlayers.get(battleSeat.getUserId())
                    .getHandCardInstances();
            battleSeat.initHandZone(handCardInstances);
        }

        decideStartPlayer(challengerPlayers);

        this.attacker = halfBattlefieldMap.get(startPlayerId);
        this.defender = halfBattlefieldMap.get(elsePlayerId);

        this.currentState = BattleStateEnum.playCards;
        // 启动状态机（非阻塞）
        advanceBattle()
                .doOnSuccess(nil -> log.info("战斗状态机正常完成"))
                .doOnError(error -> log.error("战斗状态机异常结束", error))
                .subscribe();
    }

    // region 决定开始玩家

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     */
    public void decideStartPlayer(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("决定先开始出牌的玩家，战场 {}", name);

        List<Long> playerIds = new ArrayList<>(halfBattlefieldMap.keySet());
        Long p1 = playerIds.get(0);
        Long p2 = playerIds.get(1);

        int sum1 = calculateCupRoundSum(challengerPlayers.get(p1));
        int sum2 = calculateCupRoundSum(challengerPlayers.get(p2));

        if (sum1 == sum2) {
            log.info("奖杯回合数相同，随机决定先后手");
            this.startWay = StartWayEnum.RANDOM.getValue();
            Collections.shuffle(playerIds);
        } else {
            this.startWay = StartWayEnum.NORMAL.getValue();
            if (sum1 < sum2) {
                Collections.swap(playerIds, 0, 1);
            }
            // sum1 >= sum2 时无需交换，p1 天然在前
        }

        this.startPlayerId = playerIds.get(0);
        this.elsePlayerId = playerIds.get(1);
    }

    private int calculateCupRoundSum(ChallengerPlayer player) {
        return player.getCupInstances().stream()
                .mapToInt(cup -> Integer.parseInt(cup.getRound()))
                .sum();
    }

    // endregion

    // region 战斗逻辑

    /**
     * 进攻方手牌耗尽攻击力依旧不足
     */
    private void checkAttackPower() {
        if (battle.isAttackerWeakerThanDefender()) {
            this.winnerId.setValue(defender.getUserId());
            playerMap.get(defender.getUserId()).getBattlefieldResults().put(currentRound, true);
            playerMap.get(attacker.getUserId()).getBattlefieldResults().put(currentRound, false);
            log.info("战斗结束，进攻方无多余手牌，胜利者为 {}", winnerId);

            this.currentState = BattleStateEnum.endBattle;
        } else if (battle.getDefender() != null) { // 第一张牌攻击时没有防守卡
            // TimeRange：失去旗帜
            Card card = cardMap.get(battle.getDefender().getCardId());
            if (TimeRangeEnum.LOSE_FLAG.getValue().equals(card.getTimeRange())) {
                if (card.getCheckAndPutParam() != null) {
                    this.currentState = BattleStateEnum.checkAndPut;
                    // TODO checkAndPut 跳到 moveAttackerToRestZone 这样似乎不优雅？？？
                } else {
                    this.currentState = BattleStateEnum.moveAttackerToRestZone;
                }
            }
            this.currentState = BattleStateEnum.moveAttackerToRestZone;
        } else {
            this.currentState = BattleStateEnum.moveAttackerToRestZone;
        }
    }

    /**
     * 将防守牌和防守牌底下的牌放入休息区
     */
    private void moveAttackerToRestZone() {
        if (battle.isWaitingToRest()) {
            LinkedList<CardInstance> downedCard = battle.getAwaitingRest();
            downedCard.add(battle.getDefender());

            for (CardInstance cardInstance : downedCard) {
                if (defender.addToRestZone(cardInstance)) {
                    this.winnerId.setValue(attacker.getUserId());
                    playerMap.get(attacker.getUserId()).getBattlefieldResults().put(currentRound, true);
                    playerMap.get(defender.getUserId()).getBattlefieldResults().put(currentRound, false);
                    log.info("战斗结束，防守方休息区溢出，胜利者为 {}", winnerId);

                    this.currentState = BattleStateEnum.endBattle;
                    return;
                }
            }
            battleLog.updateDefenderRestZone(defender.getUserId(), defender.getRestZone());
        }
        this.currentState = BattleStateEnum.swapAttackAndDefense;
    }

    private void swapAttackAndDefense() {
        // TODO 什么时候攻击牌会是空的呢？
        if (!battle.getAttacker().isEmpty()) { // 攻击牌非空
            Card lastAttackerCard = cardMap.get(battle.swapAttackAndDefense(tempAttackerPower).getCardId());

            if (TimeRangeEnum.CAPTURE_FLAG.getValue().equals(lastAttackerCard.getTimeRange())) { // TimeRange：夺旗成功
                ChallengerPlayer attackerInfo = playerMap.get(attacker.getUserId());
                ChallengerPlayer defenderInfo = playerMap.get(defender.getUserId());
                ConditionAndResult.apply(lastAttackerCard, currentRound, attacker, defender,
                        attackerInfo, defenderInfo, battle.getDefenderPower());
            }
            // TODO 下面这个是应该放到上面的 if 里面的吗
            // 夺旗成功上相关 buff
            int gainCoefficient = SpecialSkills.getGainCoefficient(lastAttackerCard);
            BuffCallParam buffCallParam = new BuffCallParam(TimeRangeEnum.CAPTURE_FLAG.getValue(), battle.getDefenderPower(),
                    lastAttackerCard, gainCoefficient);
            attacker.triggerRestBuffs(buffCallParam);

            BattleSeat temp = defender;
            defender = attacker;
            attacker = temp;
        }

        // 给防守方上 休息区 BUFF
        // TODO 如果上面的 if 是非必要的，下面这个卡的变量可以直接复用上面的
        if (battle.getDefender() != null) { // 跳过第一次攻击
            Card defenderCard = cardMap.get(battle.getDefender().getCardId());
            int gainCoefficient = SpecialSkills.getGainCoefficient(defenderCard);
            BuffCallParam buffCallParam = new BuffCallParam(TimeRangeEnum.CONTROL_FLAG.getValue(), battle.getDefenderPower(),
                    defenderCard, gainCoefficient);
            defender.triggerRestBuffs(buffCallParam);
        }

        battleLog.updateDefenderPower(defender.getUserId(), battle.getDefenderPower().getValue());
        this.currentState = BattleStateEnum.playCards;
    }

    private void endBattle() {
        for (ChallengerPlayer player : playerMap.values()) {
            halfBattlefieldMap.get(player.getUserId()).recallAllCards(player.getHandCardInstances());
        }

        eventPublisher.publishEvent(new EndBattleEvent(this.roomId, this.name, this.winnerId.getValue()));
        this.currentState = BattleStateEnum.outBattle;
    }

    // endregion

}
