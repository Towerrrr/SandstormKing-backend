package com.t0r.sandstormkingbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class GuestLoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户名（展示名）
     */
    private String userName;
}
