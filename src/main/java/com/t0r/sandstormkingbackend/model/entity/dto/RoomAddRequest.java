package com.t0r.sandstormkingbackend.model.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoomAddRequest implements Serializable {

    private static final long serialVersionUID = -2802323623546539956L;

    private Long ownerId;

    private String name;

}
