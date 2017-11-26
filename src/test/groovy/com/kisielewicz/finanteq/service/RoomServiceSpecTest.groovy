package com.kisielewicz.finanteq.service

import com.kisielewicz.finanteq.domain.Room
import com.kisielewicz.finanteq.exceptions.NotFoundException
import com.kisielewicz.finanteq.repository.RoomRepository
import spock.lang.Specification

class RoomServiceSpecTest extends Specification {

    private final RoomRepository roomRepository = Mock(RoomRepository)

    private final RoomService roomService = new RoomService(roomRepository)

    def "should return all rooms made"() {
        given:
        Iterable<Room> rooms = new ArrayList<>()
        Room room
        for (int i = 0; i < 5; i++) {
            room = new Room()
            room.setId(i)
            room.setIsReserved(false)
            rooms.add(room)
        }
        when:
        Iterable<Room> result = roomService.getAllRooms()
        then:
        1*roomRepository.findAll() >> rooms
        result == rooms
        result.size() == 5
    }

    def "should return empty list of reservations"() {
        given:
        Iterable<Room> rooms = new ArrayList<>()
        when:
        Iterable<Room> result = roomService.getAllRooms()
        then:
        1*roomRepository.findAll() >> rooms
        result == rooms
        result.size() == 0
    }

    def "should return all rooms which are available"() {
        given:
        Iterable<Room> rooms = new ArrayList<>()
        Room room
        for (int i = 0; i < 2; i++) {
            room = new Room()
            room.setId(i)
            room.setIsReserved(false)
            rooms.add(room)
        }
        when:
        Iterable<Room> result = roomService.getAllRoomsBasedOnReservation(false)
        then:
        1*roomRepository.findAllByIsReserved(false) >> rooms
        result == rooms
        result.size() == 2
    }

    def "should return all rooms which are unavailable"() {
        given:
        Iterable<Room> rooms = new ArrayList<>()
        Room room
        for (int i = 0; i < 2; i++) {
            room = new Room()
            room.setId(i)
            room.setIsReserved(true)
            rooms.add(room)
        }
        when:
        Iterable<Room> result = roomService.getAllRoomsBasedOnReservation(true)
        then:
        1*roomRepository.findAllByIsReserved(true) >> rooms
        result == rooms
        result.size() == 2
    }

    def "should return empty list when no available rooms"() {
        given:
        Iterable<Room> rooms = new ArrayList<>()
        Room room
        for (int i = 0; i < 2; i++) {
            room = new Room()
            room.setId(i)
            room.setIsReserved(true)
            rooms.add(room)
        }
        when:
        Iterable<Room> result = roomService.getAllRoomsBasedOnReservation(false)
        then:
        1*roomRepository.findAllByIsReserved(false) >> new ArrayList<>()
        result.size() == 0
    }

    def "should add new room"() {
        given:
        Room room = new Room()
        room.setId(1)
        room.setIsReserved(false)

        when:
        Room result = roomService.addNewRoom()
        then:
        1*roomRepository.save(_) >> room
        result == room
        result.id == room.id
    }

    def "should delete a room"() {
        given:
        int roomId = 1
        Room room = new Room()
        room.setId(1)
        room.setIsReserved(false)
        when:
        roomService.deleteRoom(roomId)
        then:
        1*roomRepository.exists(roomId) >> true
        1*roomRepository.delete(roomId)
    }

    def "should throw NotFoundException when deleting room because of no room in db"() {
        given:
        int roomId = 1
        when:
        roomService.deleteRoom(roomId)
        then:
        1*roomRepository.exists(roomId) >> false
        0*roomRepository.delete(roomId)
        NotFoundException e = thrown()
        e.getMessage() == "No room by id:1 found."
    }

    def "should change state of the room from available to unavailable"() {
        given:
        int roomId = 1
        Room room = new Room()
        room.setId(1)
        room.setIsReserved(false)
        Room shouldResult = new Room()
        shouldResult.setId(1)
        shouldResult.setIsReserved(true)
        when:
        Room result = roomService.setAvailability(roomId, false)
        then:
        1*roomRepository.exists(roomId) >> true
        1*roomRepository.findOne(roomId) >> room
        1*roomRepository.save(room) >> shouldResult
        result.getIsReserved() == shouldResult.getIsReserved()
    }

    def "should throw NotFoundException when setting availability because of no room in db"() {
        given:
        int roomId = 1
        when:
        roomService.deleteRoom(roomId)
        then:
        1*roomRepository.exists(roomId) >> false
        0*roomRepository.findOne(roomId)
        0*roomRepository.save(_)
        NotFoundException e = thrown()
        e.getMessage() == "No room by id:1 found."
    }

}
