package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

import com.t0r.sandstormkingbackend.Util.MyListUtil;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.Buff;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffCallParam;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.buff.BuffTypeEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move.Move;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.function.Consumer;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

@RequiredArgsConstructor
public class BattleSeat {

    @Getter
    private final Long userId;

    @Getter
    @Setter
    // 战斗前构筑是否就绪
    private boolean isReady = false;

    private final LinkedList<CardInstance> handZone = new LinkedList<>();

    // 卡牌名称 -> 卡牌实例列表
    public final static int MAX_REST_ZONE_SIZE = 6;
    private Map<String, LinkedList<CardInstance>> restZone = new HashMap<>();

    // 消耗牌堆
    private LinkedList<CardInstance> consumedDeck = new LinkedList<>();

    // 休息区 BUFF
    List<Consumer<BuffCallParam>> restBuffs = new ArrayList<>();
    // 下一张卡 BUFF
    Consumer<BuffCallParam> nextBuff = null;

    public void initHandZone(LinkedList<CardInstance> originalHand) {
        // TODO 这里有没有多套一层？？
        LinkedList<CardInstance> shuffled = MyListUtil.shuffleLinkedList(originalHand);
        this.handZone.addAll(shuffled);
    }

    public CardInstance castNextCard() {
        return this.handZone.removeFirst();
    }

    public boolean hasHandCards() {
        return !this.handZone.isEmpty();
    }

    public boolean addToRestZone(CardInstance cardInstance) {
        Card cardInfo = cardMap.get(cardInstance.getCardId());

        if (BuffTypeEnum.REST.getValue().equals(cardInfo.getBuffType())) { // "在休息区" 技能
            this.restBuffs.add(new Buff(cardInfo));
        }

        Move.toRestZone(cardInstance, this.restZone, this.consumedDeck);

        return this.restZone.size() > MAX_REST_ZONE_SIZE;
    }

    public void triggerRestBuffs(BuffCallParam param) {
        for (Consumer<BuffCallParam> buff : this.restBuffs) {
            buff.accept(param);
        }
    }

    public void triggerNextBuff(BuffCallParam param) {
        if (this.nextBuff != null) {
            this.nextBuff.accept(param);
            this.nextBuff = null;
        }
    }

    public boolean hasLevelCCardInRestZone() {
        return this.restZone.values().stream()
                .anyMatch(list -> !list.isEmpty() &&
                        LevelEnum.C.getValue().equals(cardMap.get(list.getFirst().getCardId()).getLevel()));
    }

}
