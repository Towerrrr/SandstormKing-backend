package com.t0r.sandstormkingbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserBatchGetRequest implements Serializable {

    private static final long serialVersionUID = 2749521610092836294L;

    private List<Long> userIdList;

}
