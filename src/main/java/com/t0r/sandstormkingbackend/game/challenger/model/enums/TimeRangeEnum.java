package com.t0r.sandstormkingbackend.game.challenger.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum TimeRangeEnum {

    PICK("当被挑选", "PICK"),

    IMMEDIATELY("立即触发", "IMMEDIATELY"),
    OPTIONAL("可选", "OPTIONAL"),
    ATTACK("攻击时", "ATTACK"),
    CONTROL_FLAG("控制旗帜", "CONTROL_FLAG"),
    FAIL_CAPTURE_FLAG("夺旗失败(无旗胜利)", "FAIL_CAPTURE_FLAG"),
    CAPTURE_FLAG("夺旗成功", "CAPTURE_FLAG"),
    LOSE_FLAG("失去旗帜", "LOSE_FLAG");

    private final String text;

    private final String value;

    TimeRangeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static TimeRangeEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TimeRangeEnum anEnum : TimeRangeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
