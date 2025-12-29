package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.Util.MyListUtil;
import com.t0r.sandstormkingbackend.Util.SpringContextHolder;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.Buff;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.Move;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.PhaseEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.StartWayEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TimeRangeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.event.CardSelectEvent;
import com.t0r.sandstormkingbackend.game.challenger.model.event.EndBattleEvent;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;
import static com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.HalfBattlefield.MAX_REST_ZONE_SIZE;

@Data
@Slf4j
@Accessors(chain = true)
public class Battlefield {

    Long roomId;

    String name;

    String currentPhase;

    Map<Long, HalfBattlefield> halfBattlefieldMap = new HashMap<>();

    Long startPlayerId;
    Long elsePlayerId;
    String startWay;

    LinkedList<Battle> battleList = new LinkedList<>();

    Long winnerId;

    private final ApplicationEventPublisher eventPublisher;

    private BattleStateEnum currentState;

    // region 战斗重构变量

    // TODO 判断有人战斗开始直接弃光手牌？

    HalfBattlefield[] halfBattlefields;
    // 0: 当前防守方, 1: 当前进攻方
    List<LinkedList<CardInstance>> handZones;
    List<List<Consumer<BuffCallParam>>> restBuffsArray;
    List<Consumer<BuffCallParam>> nextBuffs;

    int attackerIdx = 0;
    int defenderIdx = 1;
    Power defenderPower = new Power(0);
    Integer attackerPower;

    Battle battle = null;
    CardInstance attackerCard = null;
    Power tempAttackerPower = null;

    // endregion

    public Battlefield(Long roomId, String name, String currentRound, Map<Long, ChallengerPlayer> challengerPlayers,
                       ApplicationEventPublisher eventPublisher) {
        this.roomId = roomId;
        this.name = name;
        this.currentPhase = PhaseEnum.BUILD.getValue();
        for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
            String playerBattlefield = challengerPlayer.getBattlefieldSchedules().get(currentRound);
            if (playerBattlefield.equals(this.name)) {
                halfBattlefieldMap.put(challengerPlayer.getUserId(), new HalfBattlefield());
            }
        }
        this.eventPublisher = eventPublisher;
    }

    public Mono<Void> advanceBattle() {
        return Mono.defer(() -> {
                    log.info("当前战斗状态: {}", currentState);

                    switch (currentState) {
                        case firstAttack:
                            firstAttack();
                            break;
                        case triggerDefenderRestBuffs:
                            triggerDefenderRestBuffs();
                            break;
                        case castAttack:
                            castAttack();
                            break;
                        case triggerAttackerBuffs:
                            triggerAttackerBuffs();
                            break;
                        case checkAttackPower:
                            checkAttackPower();
                            break;
                        case applyAttackDamage:
                            applyAttackDamage();
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
                        case selectCard:
                            break;
                        default:
                            throw new RuntimeException("未知的战斗状态: " + currentState);
                    }

                    switch (currentState) {
                        case endBattle:
                            return Mono.empty();
                        case selectCard:
                            return selectCard(attackerIdx)
                                    .then(Mono.defer(this::advanceBattle));
                        default:
                            return Mono.defer(this::advanceBattle);
                    }
                })
                .doOnError(e -> log.error("战斗状态机异常", e));
    }

    public Long readyBattle(Long userId) {
        halfBattlefieldMap.get(userId).setReady(true);

        for (Long opponentId : halfBattlefieldMap.keySet()) {
            if (!Objects.equals(opponentId, userId)) return opponentId;
        }
        return null;
    }

    public boolean checkAllReady() {
        return halfBattlefieldMap.values().stream().allMatch(HalfBattlefield::isReady);
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

        this.currentState = BattleStateEnum.firstAttack;
        // TODO 后续重构
        halfBattlefields = new HalfBattlefield[]{
                halfBattlefieldMap.get(startPlayerId),
                halfBattlefieldMap.get(elsePlayerId)
        };
        // 0: 当前防守方, 1: 当前进攻方
        handZones = Arrays.asList(
                halfBattlefields[0].getHandZone(),
                halfBattlefields[1].getHandZone()
        );
        restBuffsArray = Arrays.asList(
                halfBattlefields[0].getRestBuffs(),
                halfBattlefields[1].getRestBuffs()
        );
        nextBuffs = Arrays.asList(
                halfBattlefields[0].getNextBuff(),
                halfBattlefields[1].getNextBuff()
        );

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

    /**
     * 最开始进攻方先放一张牌
     */
    private void firstAttack() {
        battle = new Battle();
        attackerCard = handZones.get(attackerIdx).removeFirst();
        battle.getAttacker().add(attackerCard);
        attackerPower = cardMap.get(attackerCard.getCardId()).getBasePower();

        this.currentState = BattleStateEnum.triggerDefenderRestBuffs;
    }

    private void triggerDefenderRestBuffs() {
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

        this.currentState = BattleStateEnum.castAttack;
    }

    private void castAttack() {
        // 进攻方出牌，直到攻击力 >= 防守力 或手牌用完
        if (attackerPower < Objects.requireNonNull(defenderPower).getValue() &&
                !handZones.get(attackerIdx).isEmpty()) {
            attackerCard = handZones.get(attackerIdx).removeFirst();
            tempAttackerPower = new Power(cardMap.get(attackerCard.getCardId()).getBasePower());

            this.currentState = BattleStateEnum.triggerAttackerBuffs;
        } else {
            battleList.add(battle); // 记录战斗过程
            this.currentState = BattleStateEnum.checkAttackPower;
        }
    }

    private void triggerAttackerBuffs() {
        BuffCallParam buffCallParam =
                new BuffCallParam(
                        TimeRangeEnum.ATTACK.getValue(),
                        tempAttackerPower, cardMap.get(attackerCard.getCardId()));
        for (Consumer<BuffCallParam> restBuff : restBuffsArray.get(attackerIdx)) {
            restBuff.accept(buffCallParam);
        }
        nextBuffs.get(attackerIdx).accept(buffCallParam);
        // TODO 实施“下一张卡” BUFF 技能，要结合卡的 timeRange

        this.currentState = BattleStateEnum.selectCard;
    }

    public Mono<Void> selectCard(int attackerIdx) {
        String waitKey = "user_" + whoIsAttackerOrDefender(attackerIdx);

        PlayerWaitManager playerWaitManager = SpringContextHolder.getBean(PlayerWaitManager.class);
        Mono<CardSelectorResponse> mono = playerWaitManager.createWaitMono(waitKey)
                .doOnCancel(() -> log.info("Wait cancelled for {}", waitKey));

        // TODO 判断条件占位
        eventPublisher.publishEvent( // 通知前端选牌
                new CardSelectEvent(whoIsAttackerOrDefender(attackerIdx), new CardSelectorRequest())
        );

        return mono
                .doOnSuccess(cardSelectorResponse -> {
                    // TODO 根据选择卡响应做响应操作
                })
                .then()
                .doOnSuccess(v -> this.currentState = BattleStateEnum.applyAttackDamage);
    }

    private void applyAttackDamage() {
        attackerPower += tempAttackerPower.getValue();
        battle.getAttacker().add(attackerCard);

        this.currentState = BattleStateEnum.castAttack;
    }

    /**
     * 进攻方手牌耗尽攻击力依旧不足
     */
    private void checkAttackPower() {
        if (attackerPower < defenderPower.getValue()) {
            winnerId = defenderIdx == 0 ? startPlayerId : elsePlayerId;
            log.info("战斗结束，进攻方无多余手牌，胜利者为 {}", winnerId);

            this.currentState = BattleStateEnum.endBattle;
        } else {
            this.currentState = BattleStateEnum.moveAttackerToRestZone;
        }
    }

    /**
     * 将上一轮的上一轮所有进攻牌放入休息区
     */
    private void moveAttackerToRestZone() {
        if (battleList.size() >= 2) {
            Iterator<Battle> descendingIterator = battleList.descendingIterator();
            descendingIterator.next();
            LinkedList<CardInstance> attacker = descendingIterator.next().getAttacker();

            for (CardInstance cardInstance : attacker) {
                Map<String, LinkedList<CardInstance>> restZone = halfBattlefields[defenderIdx].getRestZone();
                Card card = cardMap.get(cardInstance.getId());
                if (card.getBuffType().equals(BuffTypeEnum.REST.getValue())) { // "在休息区" 技能
                    restBuffsArray.get(defenderIdx).add(new Buff(card));
                }

                Move.toRestZone(cardInstance, restZone);
                if (restZone.size() > MAX_REST_ZONE_SIZE) {
                    winnerId = whoIsAttackerOrDefender(attackerIdx);
                    battleList.add(battle);
                    log.info("战斗结束，防守方休息区溢出，胜利者为 {}", winnerId);

                    this.currentState = BattleStateEnum.endBattle;
                    return;
                }
            }
        }

        this.currentState = BattleStateEnum.swapAttackAndDefense;
    }

    private void swapAttackAndDefense() {
        if (!battle.getAttacker().isEmpty()) {
            // 下一轮由这轮进攻方最后一张牌做防守方
            CardInstance lastAttacker = battle.getAttacker().getLast();
            battle = new Battle();
            battle.setDefender(lastAttacker);
            defenderPower.setValue(cardMap.get(lastAttacker.getCardId()).getBasePower());
            attackerPower = 0;

            int temp = defenderIdx;
            defenderIdx = attackerIdx;
            attackerIdx = temp;
        }

        this.currentState = BattleStateEnum.triggerDefenderRestBuffs;
    }

    private void endBattle() {
        eventPublisher.publishEvent(new EndBattleEvent(this.roomId, this.name, this.winnerId));
    }

    private Long whoIsAttackerOrDefender(int idx) {
        return idx == 0 ? startPlayerId : elsePlayerId;
    }

}
