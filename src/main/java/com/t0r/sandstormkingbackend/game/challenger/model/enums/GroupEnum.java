package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum GroupEnum {

    CITY("城市", "city", 0),
    BOT("机器人", "bot", 0),
    SUBMARINE("沉船", "submarine", 1),
    GHOST_HOUSE("鬼屋", "ghostHouse", 1),
    SPACE("太空", "space", 1),
    MOVIE_THEATER("电影工作室", "movieTheater", 1),
    GARDEN("游乐园", "garden", 1),
    CASTLE("城堡", "castle", 1),

    RAINBOW("彩虹", "rainbow", 2),
    TALES_FOREST("童话森林", "talesForest", 2),
    TOY_SHOP("玩具商店", "toyShop", 2),
    BEACH_CLUB("沙滩俱乐部", "beachClub", 2),
    SECRET_BASE("秘密基地", "secretBase", 2),
    UNIVERSITY("大学", "university", 2),
    HILL_TOP("山巅", "hillTop", 2);

    private final String text;

    private final String value;

    private final int version;

    GroupEnum(String text, String value, int version) {
        this.text = text;
        this.value = value;
        this.version = version;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static GroupEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (GroupEnum anEnum : GroupEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
