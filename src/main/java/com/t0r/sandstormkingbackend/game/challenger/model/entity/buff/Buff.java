package com.t0r.sandstormkingbackend.game.challenger.model.entity.buff;

import com.t0r.sandstormkingbackend.game.challenger.model.entity.Card;
import com.t0r.sandstormkingbackend.game.challenger.model.entity.battlefield.Power;

import java.util.Optional;
import java.util.function.Consumer;

public class Buff implements Consumer<BuffCallParam> {

    /**
     * 与 currentTimeRange 判断
     */
    private final String timeRange;

    /**
     * 过滤卡本身属性
     */
    private final Integer basePower;

    /**
     * 过滤卡本身属性
     */
    private final String group;

    /**
     * 过滤卡本身属性
     */
    private final String cardName;

    /**
     * buff 效果
     */
    private final Integer extraPower;

    public Buff(Card card) {
        BuffConfigParam buffConfigParam = card.getBuffConfigParam();

        this.timeRange = buffConfigParam.getTimeRange();
        this.basePower = buffConfigParam.getBasePower();
        this.group = buffConfigParam.getGroup();
        this.cardName = buffConfigParam.getCardName();
        this.extraPower = buffConfigParam.getExtraPower();
    }

    @Override
    public void accept(BuffCallParam buffCallParam) {
        String currentTimeRange = buffCallParam.getCurrentTimeRange();
        Card card = buffCallParam.getCard();
        Power currentPower = buffCallParam.getCurrentPower();

        boolean timeMatch = Optional.ofNullable(this.timeRange)
                .map(tr -> tr.equals(currentTimeRange))
                .orElse(true);

        boolean powerMatch = Optional.ofNullable(this.basePower)
                .map(bp -> bp.equals(card.getBasePower()))
                .orElse(true);

        boolean groupMatch = Optional.ofNullable(this.group)
                .map(g -> g.equals(card.getGroup()))
                .orElse(true);

        boolean nameMatch = Optional.ofNullable(this.cardName)
                .map(cn -> cn.equals(card.getName()))
                .orElse(true);

        if (timeMatch && powerMatch && groupMatch && nameMatch) {
            currentPower.add(this.extraPower);
        }
    }

}
