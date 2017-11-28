package com.kisielewicz.finanteq.web;

import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/all")
    public @ResponseBody Iterable<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/available/{available}")
    //isReserved is !available
    public @ResponseBody Iterable<Room> getAllRoomsBasedOnAvailability(@PathVariable Boolean available) {
        return roomService.getAllRoomsBasedOnReservation(!available);
    }

    @PostMapping("/add")
    public @ResponseBody Room addNewRoom() {
        return roomService.addNewRoom();
    }

    @DeleteMapping("/{roomId}/remove")
    public @ResponseBody ResponseEntity removeRoom(@PathVariable long roomId) {
        roomService.deleteRoom(roomId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{roomId}/makeAvailable")
    public @ResponseBody Room makeRoomAvailable(@PathVariable long roomId) {
        return roomService.setAvailability(roomId, true);
    }

    @PutMapping("/{roomId}/makeUnavailable")
    public @ResponseBody Room makeRoomUnavailable(@PathVariable long roomId) {
        return roomService.setAvailability(roomId, false);
    }
}
