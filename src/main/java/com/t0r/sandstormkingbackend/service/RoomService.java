package com.t0r.sandstormkingbackend.service;

import com.t0r.sandstormkingbackend.common.PageRequest;
import com.t0r.sandstormkingbackend.model.dto.room.ReadyRequest;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;

import java.util.List;

public interface RoomService {

    Room createRoom(RoomAddRequest roomAddRequest, User loginUser);

    Room joinRoom(Long roomId, User loginUser);

    List<Room> listRooms(PageRequest pageRequest);

    boolean quitRoom(Long roomId, User loginUser);

    Room getById(Long roomId);

    Boolean ready(ReadyRequest readyRequest, User loginUser);

    Boolean startGame(Long roomId, User loginUser);
}
