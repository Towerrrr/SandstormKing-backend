package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.Util.SpringContextHolder;
import com.t0r.sandstormkingbackend.game.challenger.manager.PlayerWaitManager;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorRequest;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardSelector.CardSelectorResponse;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.*;
import com.t0r.sandstormkingbackend.game.challenger.model.event.CardSelectEvent;
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
    // TODO 记得初始化当前回合数
    int currentRound;
    String name;
    String currentPhase;

    Map<Long, BattleSeat> halfBattlefieldMap = new HashMap<>();

    Long startPlayerId;
    Long elsePlayerId;
    String startWay;

    LinkedList<Battle> battleList = new LinkedList<>();

    Long winnerId;

    private final ApplicationEventPublisher eventPublisher;

    private BattleStateEnum currentState;

    // region 战斗重构变量

    // TODO 判断有人战斗开始直接弃光手牌？

    private BattleSeat attacker;
    private BattleSeat defender;

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
                halfBattlefieldMap.put(challengerPlayer.getUserId(), new BattleSeat(challengerPlayer.getUserId()));
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
                            return selectCard(attacker)
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
        return halfBattlefieldMap.values().stream().allMatch(BattleSeat::isReady);
    }

    /**
     * @param challengerPlayers 玩家 ID -> ChallengerPlayer
     */
    public void startBattle(Map<Long, ChallengerPlayer> challengerPlayers) {
        log.info("开始战斗，战场 {}", name);
        this.currentPhase = PhaseEnum.BATTLE.getValue();

        for (BattleSeat battleSeat : halfBattlefieldMap.values()) {
            LinkedList<CardInstance> handCardInstances =
                    challengerPlayers.get(battleSeat.getUserId()).getHandCardInstances();
            battleSeat.initHandZone(handCardInstances);
        }

        decideStartPlayer(challengerPlayers);

        this.attacker = halfBattlefieldMap.get(startPlayerId);
        this.defender = halfBattlefieldMap.get(elsePlayerId);

        this.currentState = BattleStateEnum.firstAttack;
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
        attackerCard = attacker.castNextCard();
        battle.getAttacker().add(attackerCard);
        attackerPower = cardMap.get(attackerCard.getCardId()).getBasePower();

        this.currentState = BattleStateEnum.triggerDefenderRestBuffs;
    }

    private void triggerDefenderRestBuffs() {
        if (battle.getDefender() != null) { // 跳过第一次攻击
            // 给防守方上 休息区 BUFF
            Card defenderCard = cardMap.get(battle.getDefender().getCardId());
            int gainCoefficient = 1;
            if (defenderCard.getName().equals(SpecialCardsEnum.STREAMER.getName())) {
                gainCoefficient = 2;
            }
            BuffCallParam buffCallParam =
                    new BuffCallParam(TimeRangeEnum.CONTROL_FLAG.getValue(), defenderPower, defenderCard, gainCoefficient);
            defender.triggerRestBuffs(buffCallParam);
        }

        this.currentState = BattleStateEnum.castAttack;
    }

    private void castAttack() {
        // 进攻方出牌，直到攻击力 >= 防守力 或手牌用完
        if (attackerPower < Objects.requireNonNull(defenderPower).getValue() &&
                attacker.hasHandCards()) {
            // TODO 控制旗帜
            attackerCard = attacker.castNextCard();
            Card card = cardMap.get(attackerCard.getCardId());
            if (card.getName().equals(SpecialCardsEnum.MACHINE.getName())) {
                tempAttackerPower = new Power(currentRound);
            } else if (card.getName().equals(SpecialCardsEnum.ZEPPELIN.getName())) {
                if (attacker.hasLevelCCardInRestZone()) {
                    this.winnerId = defender.getUserId();
                    log.info("战斗结束，进攻方打出飞艇并触发技能，胜利者为 {}", winnerId);
                    this.currentState = BattleStateEnum.endBattle;
                    return;
                }
            } else {
                tempAttackerPower = new Power(card.getBasePower());
            }
            // TODO 鹿娃

            this.currentState = BattleStateEnum.triggerAttackerBuffs;
        } else {
            battleList.add(battle); // 记录战斗过程
            this.currentState = BattleStateEnum.checkAttackPower;
        }
    }

    private void triggerAttackerBuffs() {
        Card card = cardMap.get(attackerCard.getCardId());
        int gainCoefficient = 1;
        if (card.getName().equals(SpecialCardsEnum.STREAMER.getName())) {
            gainCoefficient = 2;
        }
        BuffCallParam buffCallParam =
                new BuffCallParam(TimeRangeEnum.ATTACK.getValue(), tempAttackerPower, card, gainCoefficient);
        attacker.triggerRestBuffs(buffCallParam);
        attacker.triggerNextBuff(buffCallParam);
        // TODO 进攻时

        // TODO 实施“下一张卡” BUFF 技能，要结合卡的 timeRange

        this.currentState = BattleStateEnum.selectCard;
    }

    public Mono<Void> selectCard(BattleSeat attacker) {
        String waitKey = "user_" + attacker.getUserId();

        PlayerWaitManager playerWaitManager = SpringContextHolder.getBean(PlayerWaitManager.class);
        Mono<CardSelectorResponse> mono = playerWaitManager.createWaitMono(waitKey)
                .doOnCancel(() -> log.info("Wait cancelled for {}", waitKey));

        // TODO 判断条件占位
        eventPublisher.publishEvent( // 通知前端选牌
                new CardSelectEvent(attacker.getUserId(), new CardSelectorRequest())
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

        if (attackerPower < Objects.requireNonNull(defenderPower).getValue()) {
            // TODO 夺旗失败
        }

        this.currentState = BattleStateEnum.castAttack;
    }

    /**
     * 进攻方手牌耗尽攻击力依旧不足
     */
    private void checkAttackPower() {
        if (attackerPower < defenderPower.getValue()) {
            this.winnerId = defender.getUserId();
            log.info("战斗结束，进攻方无多余手牌，胜利者为 {}", winnerId);

            this.currentState = BattleStateEnum.endBattle;
        } else {
            // TODO 失去旗帜
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
            LinkedList<CardInstance> downedCard = descendingIterator.next().getAttacker();

            for (CardInstance cardInstance : downedCard) {
                if (defender.addToRestZone(cardInstance)) {
                    this.winnerId = attacker.getUserId();
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
            Card lastAttackerCard = cardMap.get(lastAttacker.getCardId());
            battle = new Battle();
            battle.setDefender(lastAttacker);
            if (lastAttackerCard.getName().equals(SpecialCardsEnum.MACHINE.getName())) {
                defenderPower.setValue(currentRound);
            } else {
                defenderPower.setValue(lastAttackerCard.getBasePower());
            }
            // TODO 夺旗成功逻辑
            attackerPower = 0;

            BattleSeat temp = defender;
            defender = attacker;
            attacker = temp;
        }

        this.currentState = BattleStateEnum.triggerDefenderRestBuffs;
    }

    private void endBattle() {
        eventPublisher.publishEvent(new EndBattleEvent(this.roomId, this.name, this.winnerId));
    }

}
