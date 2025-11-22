package com.t0r.sandstormkingbackend.model.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RoomMember {

    public final static String USER_ID = "userId";
    public final static String READY = "ready";

    private Long userId;

    private Boolean ready;

    // todo 断线逻辑？
//    private Boolean online;
}
