package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpecialCardsEnum {

    MACHINE("基础力量值等于当前回合数", "机械"),
    STREAMER("获得的力量值增益翻倍", "主播"),
    PACKAGE_KEEPER("要放休息区->放消耗牌堆", "背包客"),
    DWARF("要放消耗牌堆->放手牌顶", "侏儒"),
    GIANT("休息区占2位置", "巨人"),
    ZEPPELIN("休息区有C卡，立即输掉比赛", "飞艇");


    // TODO 鹿娃


    private final String text;

    private final String name;

    SpecialCardsEnum(String text, String name) {
        this.text = text;
        this.name = name;
    }

    public static SpecialCardsEnum getByName(String name) {
        if (ObjUtil.isEmpty(name)) {
            return null;
        }
        for (SpecialCardsEnum anEnum : SpecialCardsEnum.values()) {
            if (anEnum.name.equals(name)) {
                return anEnum;
            }
        }
        return null;
    }
}
