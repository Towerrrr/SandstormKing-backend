package com.t0r.sandstormkingbackend.game.challenger.model.entity.skill.move;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PermissionEnum {

    MUST("一定", "MUST"),
    OPTIONAL("可选", "OPTIONAL");

    private final String text;

    private final String value;

    PermissionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static PermissionEnum getByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PermissionEnum anEnum : PermissionEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
