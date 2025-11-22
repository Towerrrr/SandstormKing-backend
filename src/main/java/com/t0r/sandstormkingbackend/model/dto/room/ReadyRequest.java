package com.t0r.sandstormkingbackend.model.dto.room;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReadyRequest implements Serializable {

    private static final long serialVersionUID = 8095365258481918431L;

    Long roomId;

    boolean ready;

}
