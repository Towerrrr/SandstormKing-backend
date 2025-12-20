package com.t0r.sandstormkingbackend.model.dto.room;

import com.t0r.sandstormkingbackend.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class RoomRSocketRequest implements Serializable {

    private static final long serialVersionUID = -253440892636401238L;

    UserVO user;

    Long roomId;
}
