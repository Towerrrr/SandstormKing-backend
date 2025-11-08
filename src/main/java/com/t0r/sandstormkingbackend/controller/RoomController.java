package com.t0r.sandstormkingbackend.controller;

import com.t0r.sandstormkingbackend.common.BaseResponse;
import com.t0r.sandstormkingbackend.model.entity.dto.RoomAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room")
public class RoomController {

    @PostMapping("/add")
    public BaseResponse<Long> addRoom(@RequestBody RoomAddRequest roomAddRequest) {

    }
}
