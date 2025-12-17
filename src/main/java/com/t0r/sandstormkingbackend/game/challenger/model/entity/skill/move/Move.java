package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.CardInstance;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.cardFilter.CardFilter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 返回值：是否成功移动
 */
public class Move implements Function<MoveCallParam, Boolean> {

    private String permission;

    private String target;

    private String optionalStart;
    private StartEnum start;

    private Integer count;
    private Integer maxCount;

    private String type;

    private CardFilter cardFilter;

    private EndEnum end;

    @Override
    public Boolean apply(MoveCallParam moveCallParam) {
        Map<String, LinkedList<CardInstance>> mainDecks = moveCallParam.getMainDecks();
        Map<String, LinkedList<CardInstance>> discardDecks = moveCallParam.getDiscardDecks();
        LinkedList<CardInstance> handZone = moveCallParam.getHandZone();
        Map<String, LinkedList<CardInstance>> restZone = moveCallParam.getRestZone();
        List<CardInstance> consumedDeck = moveCallParam.getConsumedDeck();

        LinkedList<CardInstance> startObj = null;
        LinkedList<CardInstance> endObj = null;

        if (this.permission.equals(PermissionEnum.OPTIONAL.getValue())) {
            // TODO 前端询问是否使用
        } else if (this.permission.equals(PermissionEnum.MUST.getValue())) {
            if (this.start == null && this.optionalStart.equals(OptionalStartEnum.HAND_ZONE.getValue())) {
                // TODO 所有需前端选择的情况占位
            } else if (this.optionalStart == null) {
                switch (this.start) {
                    case THIS_CARD:
                        // TODO 本卡逻辑待定
                        break;
                    case HAND_ZONE_TOP:
                        startObj = handZone;
                        break;
                    case A_MAIN_DECK:
                        startObj = mainDecks.get("A");
                        break;
                    case B_MAIN_DECK:
                        startObj = mainDecks.get("B");
                        break;
                    case C_MAIN_DECK:
                        startObj = mainDecks.get("C");
                        break;
                    case HAND_ZONE_BOTTOM:
                        // TODO 手牌底的情况最后写
                        break;
                    default:
                        throw new RuntimeException("move start error");
                }
                // TODO maxCount、type、filter 的情况先不写
                switch (this.end) {
                    case HAND_ZONE_TOP:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = startObj.removeFirst();
                            handZone.addFirst(cardInstance);
                        }
                        break;
                    case HAND_ZONE_BOTTOM:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = startObj.removeFirst();
                            handZone.addLast(cardInstance);
                        }
                        break;
                    case REST_ZONE:
                        // TODO 休息区逻辑待定
                        break;
                    case CONSUMED_DECK:
                        for (int i = 0; i < this.count; i++) {
                            CardInstance cardInstance = startObj.removeFirst();
                            consumedDeck.add(cardInstance);
                        }
                        break;
                    case DISCARD_DECK:
                        // TODO 弃牌堆逻辑待定
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
