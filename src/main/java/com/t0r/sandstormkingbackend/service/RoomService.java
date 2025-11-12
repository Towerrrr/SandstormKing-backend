package com.t0r.sandstormkingbackend.service;

import com.t0r.sandstormkingbackend.model.dto.room.RoomAddRequest;
import com.t0r.sandstormkingbackend.model.entity.Room;
import com.t0r.sandstormkingbackend.model.entity.User;

public interface RoomService {

    Room addRoom(RoomAddRequest roomAddRequest, User loginUser);

    Boolean joinRoom(Long roomId, User loginUser);



}
