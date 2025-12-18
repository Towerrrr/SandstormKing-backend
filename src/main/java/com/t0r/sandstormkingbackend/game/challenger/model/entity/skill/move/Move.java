package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.RoomGameState;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;
import com.t0r.sandstormkingbackend.game.challenger.model.enums.LevelEnum;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.t0r.sandstormkingbackend.game.challenger.handler.ChallengerGameManager.cardMap;

/**
 * 返回值：是否成功移动
 */
public class Move implements Function<MoveCallParam, Boolean> {

    private String permission;

    private String target;

    // TODO 注意从主牌堆移动的话也要移动到手牌中
    private String optionalStart;
    private StartEnum start;
    private CardInstance thisCard;

    private Integer count;
    private Integer maxCount;

    private String type;

    private CardFilter cardFilter;

    private EndEnum end;

    public static void toRestZone(CardInstance cardInstance, Map<String, LinkedList<CardInstance>> restZone) {
        String name = cardMap.get(cardInstance.getId()).getName();
        restZone.putIfAbsent(name, new LinkedList<>());
        restZone.get(name).add(cardInstance);
    }

    @Override
    public Boolean apply(MoveCallParam moveCallParam) {
        Map<String, LinkedList<CardInstance>> mainDecks = moveCallParam.getMainDecks();
        Map<String, LinkedList<CardInstance>> discardDecks = moveCallParam.getDiscardDecks();
        LinkedList<CardInstance> handCardInstances = moveCallParam.getHandCardInstances();
        LinkedList<CardInstance> handZone = moveCallParam.getHandZone();
        Map<String, LinkedList<CardInstance>> restZone = moveCallParam.getRestZone();
        LinkedList<CardInstance> consumedDeck = moveCallParam.getConsumedDeck();

        LinkedList<CardInstance> startObj = new LinkedList<>();

        if (this.permission.equals(PermissionEnum.OPTIONAL.getValue())) {
            // TODO 前端询问是否使用
        } else if (this.permission.equals(PermissionEnum.MUST.getValue())) {
            if (this.start == null && this.optionalStart.equals(OptionalStartEnum.HAND_ZONE.getValue())) {
                // TODO 所有需前端选择的情况占位
            } else if (this.optionalStart == null) {
                switch (this.start) {
                    case THIS_CARD:
                        startObj.add(thisCard);
                        break;
                    case HAND_ZONE_TOP:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = handZone.removeFirst();
                            startObj.add(cardInstance);
                        }
                        break;
                    case A_MAIN_DECK:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = mainDecks.get(LevelEnum.A.getValue()).removeFirst();
                            startObj.add(cardInstance);
                            handCardInstances.add(cardInstance);
                        }
                        break;
                    case B_MAIN_DECK:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = mainDecks.get(LevelEnum.B.getValue()).removeFirst();
                            startObj.add(cardInstance);
                            handCardInstances.add(cardInstance);
                        }
                        break;
                    case C_MAIN_DECK:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = mainDecks.get(LevelEnum.C.getValue()).removeFirst();
                            startObj.add(cardInstance);
                            handCardInstances.add(cardInstance);
                        }
                        break;
                    case HAND_ZONE_BOTTOM:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = handZone.removeLast();
                            startObj.add(cardInstance);
                        }
                        break;
                    default:
                        throw new RuntimeException("move start error");
                }
                // TODO maxCount、type、filter 的情况先不写
                switch (this.end) {
                    case HAND_ZONE_TOP:
                        while (!startObj.isEmpty()) {
                            handZone.addFirst(startObj.removeFirst());
                        }
                        break;
                    case HAND_ZONE_BOTTOM:
                        while (!startObj.isEmpty()) {
                            handZone.addLast(startObj.removeFirst());
                        }
                        break;
                    case REST_ZONE:
                        while (!startObj.isEmpty()) {
                            Move.toRestZone(startObj.removeFirst(), restZone);
                        }
                        break;
                    case CONSUMED_DECK:
                        while (!startObj.isEmpty()) {
                            consumedDeck.add(startObj.removeFirst());
                        }
                        break;
                    case DISCARD_DECK:
                        Set<Integer> cardIds = startObj.stream().map(CardInstance::getId).collect(Collectors.toSet());
                        RoomGameState.discardCardInstances(handCardInstances, cardIds, discardDecks);
                        break;
                    default:
                        throw new RuntimeException("move end error");
                }
            }
        } else {
            throw new RuntimeException("move permission error");
        }
        return null;
    }
}
