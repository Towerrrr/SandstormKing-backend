package com.t0r.sandstormkingbackend.controller;

import com.t0r.sandstormkingbackend.common.BaseResponse;
import com.t0r.sandstormkingbackend.common.ResultUtils;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.service.RoomService;
import com.t0r.sandstormkingbackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Resource
    private UserService userService;

    @Resource
    private RoomService roomService;

    @PostMapping("/add")
    public BaseResponse<Room> addRoom(@RequestBody RoomAddRequest roomAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Room room = roomService.addRoom(roomAddRequest, loginUser);
        return ResultUtils.success(room);
    }


}
