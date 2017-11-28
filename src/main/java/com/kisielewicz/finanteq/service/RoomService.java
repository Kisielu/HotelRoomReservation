package com.kisielewicz.finanteq.service;

import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.exceptions.NotFoundException;
import com.kisielewicz.finanteq.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);

    private static final String ROOM_NOT_FOUND = "No room by id:%s found.";

    private final RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Iterable<Room> getAllRooms() {
        LOGGER.info("Getting all rooms from DB");
        return roomRepository.findAll();
    }

    public Iterable<Room> getAllRoomsBasedOnReservation(Boolean isReserved) {
        LOGGER.info("Getting all rooms by reserved status: {}", isReserved);
        return roomRepository.findAllByIsReserved(isReserved);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Room addNewRoom() {
        LOGGER.info("Creating new room");
        Room newRoom = new Room();
        newRoom.setIsReserved(false);
        return roomRepository.save(newRoom);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteRoom(long roomId) {
        LOGGER.info("Deleting a room with id: {}", roomId);
        if (roomExists(roomId)) {
            roomRepository.delete(roomId);
            LOGGER.info("Successfully deleted a room");
        } else {
            LOGGER.info("Didn't find a room for id: {}, returning 404 exception.", roomId);
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, roomId));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Room setAvailability(long roomId, boolean available) {
        LOGGER.info("setting room availability to {}", available);
        if (roomExists(roomId)) {
            Room room = roomRepository.findOne(roomId);
            room.setIsReserved(!available);
            LOGGER.info("Successfully set availability");
            return roomRepository.save(room);
        } else {
            LOGGER.info("Didn't find a room for id: {}, returning 404 exception.", roomId);
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, roomId));
        }

    }

    private boolean roomExists(long roomId) {
        LOGGER.info("Checking if room for id: {} exists.", roomId);
        return roomRepository.exists(roomId);
    }
}
