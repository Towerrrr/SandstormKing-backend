package com.t0r.sandstormkingbackend.game.challenger.model.entity;

import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 负责主牌堆 / 弃牌堆 的管理：初始化、抽牌、弃牌、洗牌等操作。
 */
public class DeckManager {

    private final Map<String, Deque<CardInstance>> mainDecks = new ConcurrentHashMap<>();
    private final Map<String, Deque<CardInstance>> discardDecks = new ConcurrentHashMap<>();

    // cardId -> Card 元数据，来自外部（ChallengerGameManager.cardMap）
    private final Map<Integer, Card> cardMap;

    public DeckManager(Map<Integer, Card> cardMap) {
        this.cardMap = cardMap;
        initLevelMaps();
    }

    private void initLevelMaps() {
        for (LevelEnum level : LevelEnum.values()) {
            if (level.isKept()) {
                mainDecks.put(level.getValue(), new ConcurrentLinkedDeque<>());
                discardDecks.put(level.getValue(), new ConcurrentLinkedDeque<>());
            }
        }
    }

    /**
     * 根据 cardMap 填充主牌堆并给非保留等级卡片分配到玩家手牌。
     * 返回最后使用的本地 id（从1开始计数，等于总张数）。
     */
    public int initCardInstances(Map<Long, ChallengerPlayer> challengerPlayers) {
        int localId = 1;

        for (Card card : cardMap.values()) {
            String cardLevel = card.getLevel();
            LevelEnum levelEnum = LevelEnum.getEnumByValue(cardLevel);

            if (levelEnum != null && levelEnum.isKept()) {
                int count = card.getCount() != null ? card.getCount() : 1;
                for (int i = 0; i < count; i++) {
                    CardInstance instance = new CardInstance();
                    instance.setId(localId++);
                    instance.setCardId(card.getId());
                    mainDecks.get(cardLevel).add(instance);
                }
            } else if (levelEnum != null) {
                for (ChallengerPlayer challengerPlayer : challengerPlayers.values()) {
                    for (int i = 0; i < card.getCount(); i++) {
                        CardInstance instance = new CardInstance();
                        instance.setId(localId++);
                        instance.setCardId(card.getId());
                        challengerPlayer.getHandCardInstances().add(instance);
                    }
                }
            }
        }

        // 打乱主牌堆
        for (LevelEnum levelEnum : LevelEnum.values()) {
            if (levelEnum.isKept()) {
                shuffleDeck(mainDecks.get(levelEnum.getValue()));
            }
        }

        return localId - 1;
    }

    private void shuffleDeck(Deque<CardInstance> deck) {
        if (deck == null || deck.size() <= 1) {
            return;
        }

        List<CardInstance> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled);
        deck.clear();
        deck.addAll(shuffled);
    }

    /**
     * 从指定等级的主牌堆抽取若干张，保证在不足时先把弃牌堆洗回主牌堆。
     */
    public LinkedList<CardInstance> draw(String level, int count) {
        final LinkedList<CardInstance> result = new LinkedList<>();
        if (!mainDecks.containsKey(level)) {
            return result;
        }

        Deque<CardInstance> mainDeck = mainDecks.get(level);
        Deque<CardInstance> discardDeck = discardDecks.get(level);

        if (mainDeck.size() < count) {
            shuffleDeck(discardDeck);
            mainDeck.addAll(discardDeck);
            discardDeck.clear();
        }

        for (int i = 0; i < count && !mainDeck.isEmpty(); i++) {
            result.add(mainDeck.removeFirst());
        }

        return result;
    }

    /**
     * 处理临时选择：把选中的放入 selectedCards，未选中的放入弃牌堆，清空 temp 列表。
     */
    public void processSelection(ChallengerPlayer currentPlayer, Set<Integer> selectedCardInstanceIds) {
        LinkedList<CardInstance> tempSelected = currentPlayer.getTempSelectedCardInstances();
        Set<CardInstance> selectedCards = currentPlayer.getSelectedCards();
        for (CardInstance cardInstance : tempSelected) {
            String level = cardMap.get(cardInstance.getCardId()).getLevel();
            if (selectedCardInstanceIds != null && selectedCardInstanceIds.contains(cardInstance.getId())) {
                selectedCards.add(cardInstance);
            } else {
                discardDecks.get(level).add(cardInstance);
            }
        }
        tempSelected.clear();
    }

    /**
     * 从玩家手牌中弃牌，符合保留等级则放入对应弃牌堆。
     */
    public void discardFromHand(LinkedList<CardInstance> handCardInstances,
            Set<Integer> cardInstanceIds) {
        Iterator<CardInstance> iterator = handCardInstances.iterator();
        while (iterator.hasNext()) {
            CardInstance cardInstance = iterator.next();
            if (cardInstanceIds.contains(cardInstance.getId())) {
                iterator.remove();
                String level = cardMap.get(cardInstance.getCardId()).getLevel();
                if (LevelEnum.valueOf(level).isKept()) {
                    discardDecks.get(level).add(cardInstance);
                }
            }
        }
    }

    public Map<String, Deque<CardInstance>> getMainDecks() {
        return mainDecks;
    }

    public Map<String, Deque<CardInstance>> getDiscardDecks() {
        return discardDecks;
    }
}
