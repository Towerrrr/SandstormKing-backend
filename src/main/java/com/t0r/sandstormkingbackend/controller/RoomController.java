package com.t0r.sandstormkingbackend.controller;

import com.t0r.sandstormkingbackend.common.BaseResponse;
import com.t0r.sandstormkingbackend.common.PageRequest;
import com.t0r.sandstormkingbackend.common.ResultUtils;
import com.t0r.sandstormkingbackend.exception.ErrorCode;
import com.t0r.sandstormkingbackend.exception.ThrowUtils;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;
import com.t0r.sandstormkingbackend.service.RoomService;
import com.t0r.sandstormkingbackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Resource
    private UserService userService;

    @Resource
    private RoomService roomService;

    @GetMapping("/list")
    public BaseResponse<List<Room>> listRooms(PageRequest pageRequest, HttpServletRequest request) {
        List<Room> roomList = roomService.listRooms(pageRequest);
        return ResultUtils.success(roomList);
    }

    @PostMapping("/add")
    public BaseResponse<Room> addRoom(@RequestBody RoomAddRequest roomAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Room room = roomService.createRoom(roomAddRequest, loginUser);
        return ResultUtils.success(room);
    }

    @GetMapping("/join")
    public BaseResponse<Room> joinRoom(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Room room = roomService.joinRoom(roomId, loginUser);
        return ResultUtils.success(room);
    }

    @GetMapping("/quit")
    public BaseResponse<Boolean> quitRoom(@RequestParam Long roomId, HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean isQuit = roomService.quitRoom(roomId, loginUser);
        return ResultUtils.success(isQuit);
    }

}
