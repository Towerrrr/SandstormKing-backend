package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.BattleSeat;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.SpecialCardsEnum;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

/**
 * 返回值：是否成功移动
 */
public class Move implements Function<BattleSeat, Boolean> {

    private final String optionalStart;
    private final StartEnum start;
    // TODO 待处理
    private CardInstance thisCard;

    private final Integer count;
    private final Integer maxCount;

    private final EndEnum end;

    public Move(MoveConfigParam moveConfigParam) {
        this.optionalStart = moveConfigParam.getOptionalStart();
        this.start = StartEnum.getByValue(moveConfigParam.getStart());
        this.count = moveConfigParam.getCount();
        this.maxCount = moveConfigParam.getMaxCount();
        this.end = EndEnum.getByValue(moveConfigParam.getEnd());
    }

    /**
     * “背包客”技能在这里实现
     */
    public static void toRestZone(CardInstance cardInstance,
            Map<String, LinkedList<CardInstance>> restZone,
            LinkedList<CardInstance> consumedDeck) {
        String name = cardMap.get(cardInstance.getCardId()).getName();
        if (name.equals(SpecialCardsEnum.PACKAGE_KEEPER.getName())) {
            consumedDeck.add(cardInstance);
            return;
        }
        if (name.equals(SpecialCardsEnum.GIANT.getName())) {
            // 用一个不放卡的位置表示巨人占 2 个位置
            restZone.putIfAbsent("巨人(占位)", new LinkedList<>());
        }

        restZone.putIfAbsent(name, new LinkedList<>());
        restZone.get(name).add(cardInstance);
    }

    /**
     * “侏儒”技能在这里实现
     */
    public static void toConsumedDeck(CardInstance cardInstance,
            LinkedList<CardInstance> consumedDeck,
            LinkedList<CardInstance> handZone) {
        String name = cardMap.get(cardInstance.getCardId()).getName();
        if (name.equals(SpecialCardsEnum.DWARF.getName())) {
            handZone.offerFirst(cardInstance);
            return;
        }

        consumedDeck.offer(cardInstance);
    }

    @Override
    public Boolean apply(BattleSeat battleSeat) {

        LinkedList<CardInstance> startObj = getStartCards(battleSeat);

        // TODO: 这里可以插入 maxCount、type、filter 相关处理

        moveToEnd(startObj, battleSeat);
        return true;
    }

    public Boolean apply(BattleSeat battleSeat, CardInstance cardInstance) {
        LinkedList<CardInstance> startObj = new LinkedList<>(Collections.singletonList(cardInstance));
        moveToEnd(startObj, battleSeat);
        return true;
    }

    public Boolean apply(BattleSeat battleSeat, LinkedList<CardInstance> cardInstances) {
        ThrowUtils.throwIf(cardInstances.size() > this.maxCount,
                ErrorCode.PARAMS_ERROR, "选择的卡牌数不可超过" + this.maxCount);
        moveToEnd(cardInstances, battleSeat);
        return true;
    }

    private LinkedList<CardInstance> getStartCards(BattleSeat battleSeat) {
        LinkedList<CardInstance> startObj = new LinkedList<>();
        Map<String, Deque<CardInstance>> mainDecks = battleSeat.getMainDecks();
        LinkedList<CardInstance> handZone = battleSeat.getHandZone();

        if (this.start == null && OptionalStartEnum.HAND_ZONE.getValue().equals(this.optionalStart)) {
            // TODO: 前端选择，当前不支持
            throw new UnsupportedOperationException("前端选择起点尚未实现");
        } else if (this.optionalStart == null) {
            if (this.start != null) {
                switch (this.start) {
                    case THIS_CARD:
                        startObj.add(thisCard);
                        break;
                    case HAND_ZONE_TOP:
                        moveCards(handZone, startObj, true, this.count);
                        break;
                    case HAND_ZONE_BOTTOM:
                        moveCards(handZone, startObj, false, this.count);
                        break;
                    case A_MAIN_DECK:
                        moveCards(mainDecks.get(LevelEnum.A.getValue()), startObj, true, this.count);
                        break;
                    case B_MAIN_DECK:
                        moveCards(mainDecks.get(LevelEnum.B.getValue()), startObj, true, this.count);
                        break;
                    case C_MAIN_DECK:
                        moveCards(mainDecks.get(LevelEnum.C.getValue()), startObj, true, this.count);
                        break;
                    default:
                        throw new RuntimeException("move start error");
                }
            }
        }
        return startObj;
    }

    private void moveCards(Deque<CardInstance> from, LinkedList<CardInstance> to, boolean fromFirst, int count) {
        for (int i = 0; i < count; i++) {
            if (from.isEmpty()) {
                break;
            }
            to.add(fromFirst ? from.removeFirst() : from.removeLast());
        }
    }

    private void moveToEnd(LinkedList<CardInstance> cards, BattleSeat battleSeat) {
        LinkedList<CardInstance> handZone = battleSeat.getHandZone();
        Map<String, LinkedList<CardInstance>> restZone = battleSeat.getRestZone();
        LinkedList<CardInstance> consumedDeck = battleSeat.getConsumedDeck();
        Map<String, Deque<CardInstance>> discardDecks = battleSeat.getDiscardDecks();

        switch (this.end) {
            case HAND_ZONE_TOP:
                handZone.addAll(0, cards);
                break;
            case HAND_ZONE_BOTTOM:
                handZone.addAll(cards);
                break;
            case REST_ZONE:
                for (CardInstance card : cards) {
                    toRestZone(card, restZone, consumedDeck);
                }
                break;
            case CONSUMED_DECK:
                for (CardInstance card : cards) {
                    toConsumedDeck(card, consumedDeck, handZone);
                }
                break;
            case DISCARD_DECK:
                for (CardInstance card : cards) {
                    String level = cardMap.get(card.getCardId()).getLevel();
                    discardDecks.get(level).add(card);
                }
                break;
            default:
                throw new RuntimeException("move end error");
        }
    }
}