package com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield;

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
    private final Map<String, Deque<CardInstance>> mainDecks;
    @Getter
    private final Map<String, Deque<CardInstance>> discardDecks;

    @Getter
    @Setter
    // 战斗前构筑是否就绪
    private boolean isReady = false;

    @Getter
    private final LinkedList<CardInstance> handZone = new LinkedList<>();

    // 卡牌名称 -> 卡牌实例列表
    public final static int MAX_REST_ZONE_SIZE = 6;
    @Getter
    private final Map<String, LinkedList<CardInstance>> restZone = new LinkedHashMap<>();

    // 消耗牌堆
    @Getter
    private final LinkedList<CardInstance> consumedDeck = new LinkedList<>();

    // 休息区 BUFF
    List<Consumer<BuffCallParam>> restBuffs = new ArrayList<>();
    // 下一张卡 BUFF
    Consumer<BuffCallParam> nextBuff = null;

    public void initHandZone(LinkedList<CardInstance> originalHand) {
        if (originalHand == null || originalHand.isEmpty()) {
            return;
        }

        Collections.shuffle(originalHand);
        this.handZone.addAll(originalHand);
        originalHand.clear();
    }

    public void recallAllCards(LinkedList<CardInstance> originalHand) {
        if (originalHand == null) {
            return;
        }

        originalHand.addAll(this.handZone);
        this.handZone.clear();
        originalHand.addAll(this.consumedDeck);
        this.consumedDeck.clear();

        for (LinkedList<CardInstance> cardList : this.restZone.values()) {
            if (cardList != null && !cardList.isEmpty()) {
                originalHand.addAll(cardList);
            }
        }
        this.restZone.clear();
        this.restBuffs.clear();

        originalHand.sort(Comparator.comparing(CardInstance::getId));
    }

    public CardInstance castNextCard() {
        return this.handZone.removeFirst();
    }

    public boolean hasCardInHandZone() {
        return !this.handZone.isEmpty();
    }

    public boolean hasCardInConsumedDeck() {
        return !this.consumedDeck.isEmpty();
    }

    public int getHandZoneSize() {
        return this.handZone.size();
    }

    public int getConsumedDeckSize() {
        return this.consumedDeck.size();
    }

    public LinkedList<CardInstance> popTopHandZone(int n) {
        LinkedList<CardInstance> result = new LinkedList<>();
        if (n <= 0 || this.handZone.isEmpty()) {
            return result;
        }

        while (n > 0 && !this.handZone.isEmpty()) { // 支持剩余手牌仅剩小于 n 张的情况
            result.add(this.handZone.removeFirst());
            n--;
        }
        return result;
    }

    public LinkedList<CardInstance> popBottomHandZone(int n) {
        LinkedList<CardInstance> result = new LinkedList<>();
        if (n <= 0 || this.handZone.isEmpty()) {
            return result;
        }

        while (n > 0 && !this.handZone.isEmpty()) { // 支持剩余手牌仅剩小于 n 张的情况
            result.add(this.handZone.removeLast());
            n--;
        }
        return result;
    }

    public void addCardsToHandZoneHead(LinkedList<CardInstance> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        this.handZone.addAll(0, cards);
    }

    public void addCardsToHandZoneTail(LinkedList<CardInstance> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        this.handZone.addAll(cards);
    }

    public void addCardsToConsumedDeck(LinkedList<CardInstance> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        for (CardInstance card : cards) {
            Move.toConsumedDeck(card, this.consumedDeck, this.handZone);
        }
    }

    public boolean addToRestZone(CardInstance cardInstance) {
        Card cardInfo = cardMap.get(cardInstance.getCardId());

        if (BuffTypeEnum.REST.getValue().equals(cardInfo.getBuffType())) { // "在休息区" 技能
            this.restBuffs.add(new Buff(cardInfo));
        }

        Move.toRestZone(cardInstance, this.restZone, this.consumedDeck);

        return this.restZone.size() > MAX_REST_ZONE_SIZE;
    }

    public CardInstance removeCardFromRestZone(Integer instanceId) {
        for (LinkedList<CardInstance> cardList : this.restZone.values()) {
            if (cardList == null || cardList.isEmpty()) {
                continue;
            }

            Iterator<CardInstance> it = cardList.iterator();
            while (it.hasNext()) {
                CardInstance cardInstance = it.next();
                if (cardInstance.getId().equals(instanceId)) {
                    Card card = cardMap.get(cardInstance.getCardId());
                    if (BuffTypeEnum.REST.getValue().equals(card.getBuffType())) {
                        removeBuffByCardId(card.getId());
                    }
                    it.remove();
                    return cardInstance;
                }
            }
        }
        return null;
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

    private void removeBuffByCardId(Integer cardId) {
        if (cardId == null || this.restBuffs.isEmpty()) {
            return;
        }

        Iterator<Consumer<BuffCallParam>> iterator = this.restBuffs.iterator();
        while (iterator.hasNext()) {
            Consumer<BuffCallParam> consumer = iterator.next();
            if (consumer instanceof Buff) {
                Buff buff = (Buff) consumer;
                if (cardId.equals(buff.getCardId())) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    public LinkedList<CardInstance> getAllCardsInRestZone() {
        LinkedList<CardInstance> allCards = new LinkedList<>();

        if (this.restZone.isEmpty()) {
            return allCards;
        }

        for (LinkedList<CardInstance> cardList : this.restZone.values()) {
            if (cardList != null && !cardList.isEmpty()) {
                allCards.addAll(cardList);
            }
        }

        return allCards;
    }

    public boolean hasGroupInRestZone(String groupName) {
        if (groupName == null) {
            return false;
        }

        return this.restZone.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> cardMap.get(list.getFirst().getCardId()))
                .filter(card -> card != null && card.getGroup() != null)
                .anyMatch(card -> groupName.equals(card.getGroup()));
    }

    public int getGroupCountInRestZone() {
        return (int) this.restZone.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> cardMap.get(list.getFirst().getCardId()))
                .filter(card -> card != null && card.getGroup() != null)
                .map(Card::getGroup)
                .distinct()
                .count();
    }

    public int getBasePowerCountInRestZone() {
        return (int) this.restZone.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> cardMap.get(list.getFirst().getCardId()))
                .filter(card -> card != null && card.getBasePower() != null)
                .map(Card::getBasePower)
                .distinct()
                .count();
    }

    public int getCardCountByGroupInRestZone(String group) {
        if (group == null) {
            return 0;
        }

        return this.restZone.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .filter(list -> {
                    Card card = cardMap.get(list.getFirst().getCardId());
                    return card != null && group.equals(card.getGroup());
                })
                .mapToInt(LinkedList::size)
                .sum();
    }

    public boolean hasLevelCCardInRestZone() {
        return this.restZone.values().stream()
                .anyMatch(list -> !list.isEmpty() &&
                        LevelEnum.C.getValue().equals(cardMap.get(list.getFirst().getCardId()).getLevel()));
    }

    public boolean hasRookieInRestZone() {
        return this.restZone.values().stream()
                .anyMatch(list -> !list.isEmpty() &&
                        ("新丁").equals(cardMap.get(list.getFirst().getCardId()).getName()));
    }

}
