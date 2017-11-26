package com.kisielewicz.finanteq.service;

import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.exceptions.NotFoundException;
import com.kisielewicz.finanteq.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final String ROOM_NOT_FOUND = "No room by id:%s found.";

    private RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Iterable<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Iterable<Room> getAllRoomsBasedOnReservation(Boolean isReserved) {
        return roomRepository.findAllByIsReserved(isReserved);
    }

    @Transactional
    public Room addNewRoom() {
        Room newRoom = new Room();
        newRoom.setIsReserved(false);
        return roomRepository.save(newRoom);
    }

    @Transactional
    public void deleteRoom(long roomId) {
        if (roomExists(roomId)) {
            roomRepository.delete(roomId);
        } else {
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, roomId));
        }
    }

    @Transactional
    public Room setAvailability(long roomId, boolean available) {
        if (roomExists(roomId)) {
            Room room = roomRepository.findOne(roomId);
            room.setIsReserved(!available);
            return roomRepository.save(room);
        } else {
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, roomId));
        }

    }

    private boolean roomExists(long roomId) {
        return roomRepository.exists(roomId);
    }
}
