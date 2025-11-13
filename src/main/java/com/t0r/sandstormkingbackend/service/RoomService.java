package com.t0r.sandstormkingbackend.service;

import com.t0r.sandstormkingbackend.common.PageRequest;
import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;

import java.util.List;

public interface RoomService {

    Room addRoom(RoomAddRequest roomAddRequest, User loginUser);

    Boolean joinRoom(Long roomId, User loginUser);

    Room joinRoom(String roomId, User loginUser);

    List<Room> listRooms(PageRequest pageRequest);

}
