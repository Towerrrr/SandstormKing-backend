package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;


import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.ChallengerPlayer;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.ConditionAndResult.ConditionAndResult;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.Move;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveConfigParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.MoveTargetEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.SkillTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.TimeRangeEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

/**
 * 出牌
 */
@Slf4j
@AllArgsConstructor
public class PlayCards {

    private String currentRound;

    private Battle battle;
    private BattleSeat attacker;
    private BattleSeat defender;
    private CardInstance attackerCard;
    private Power tempAttackerPower;

    private Map<Long, ChallengerPlayer> playerMap;
    private WinnerId winnerId;

    public BattleStateEnum castAttack() {
        // 进攻方出牌，第一次出牌，或者，攻击力 < 防守力 且 手牌未空
        while (battle.isFirstAttack() ||
                battle.isAttackerWeakerThanDefender() && attacker.hasCardInHandZone()) {
            // TODO 控制旗帜
            attackerCard = attacker.castNextCard();
            Card card = cardMap.get(attackerCard.getCardId());

            if (SpecialSkills.checkInstantWin(card, attacker)) {
                this.winnerId.setValue(defender.getUserId());
                playerMap.get(defender.getUserId()).getBattlefieldResults().put(currentRound, true);
                playerMap.get(attacker.getUserId()).getBattlefieldResults().put(currentRound, false);
                log.info("战斗结束，进攻方打出飞艇并触发技能，胜利者为 {}", winnerId);
                return BattleStateEnum.endBattle;
            }

            tempAttackerPower = new Power(SpecialSkills.calculateRealTimePower(card, currentRound));
            // TODO 鹿娃

            if (card.getTimeRange().equals(TimeRangeEnum.IMMEDIATELY.getValue())) { // TimeRange：立即触发
                if (SkillTypeEnum.CONDITION_AND_RESULT.getValue().equals(card.getSkillType())) {
                    ChallengerPlayer attackerInfo = playerMap.get(attacker.getUserId());
                    ChallengerPlayer defenderInfo = playerMap.get(defender.getUserId());
                    ConditionAndResult.apply(card, currentRound, attacker, defender,
                            attackerInfo, defenderInfo, tempAttackerPower);
                }
                if (SkillTypeEnum.CHECK_AND_MOVE_RESULT.getValue().equals(card.getSkillType())) {
                    return BattleStateEnum.checkAndPut;
                }
                if (SkillTypeEnum.SELECTOR_AND_MOVE.getValue().equals(card.getSkillType())) {
                    return BattleStateEnum.selectCard;
                }
                if (SkillTypeEnum.IMMEDIATELY_MOVE.getValue().equals(card.getSkillType())) {
                    MoveConfigParam moveConfigParam = card.getMoveConfigParam();
                    if (moveConfigParam != null) {
                        Move move = new Move(moveConfigParam);
                        if (MoveTargetEnum.OPPONENT.equals(card.getMoveTargetEnum())) {
                            move.apply(defender);
                        } else { // 默认移动自己的
                            move.apply(attacker);
                        }
                    }
                }
            }
            if (card.getTimeRange().equals(TimeRangeEnum.OPTIONAL.getValue())) { // TimeRange：可选的
                // 当前可选的技能全都是选择卡牌
                return BattleStateEnum.selectCard;
            }
            triggerAttackerBuffs();
            applyAttackDamage();
        }

        return BattleStateEnum.checkAttackPower;
    }

    private void triggerAttackerBuffs() {
        Card card = cardMap.get(attackerCard.getCardId());
        int gainCoefficient = SpecialSkills.getGainCoefficient(card);
        BuffCallParam buffCallParam = new BuffCallParam(TimeRangeEnum.ATTACK.getValue(), tempAttackerPower, card,
                gainCoefficient);
        attacker.triggerRestBuffs(buffCallParam);
        attacker.triggerNextBuff(buffCallParam);
        // TODO 进攻时

        // TODO 实施“下一张卡” BUFF 技能，要结合卡的 timeRange

    }

    private void applyAttackDamage() {
        battle.addAttackerPower(tempAttackerPower.getTempValue());
        battle.addAttackerCard(attackerCard);

        if (battle.isAttackerWeakerThanDefender()) {
            // TODO 夺旗失败
        }
    }

}
