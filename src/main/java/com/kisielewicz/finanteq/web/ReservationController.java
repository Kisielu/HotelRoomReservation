package com.kisielewicz.finanteq.web;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.dto.ReservationDTO;
import com.kisielewicz.finanteq.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/all")
    public @ResponseBody
    Iterable<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @GetMapping("/{reservationId}")
    public @ResponseBody Reservation getReservationDetails(@PathVariable long reservationId) {
        return reservationService.getReservation(reservationId);
    }

    @GetMapping("/room/{roomId}")
    public @ResponseBody
    Iterable<Reservation> getReservationsForRoom(@PathVariable long roomId) {
        return reservationService.getAllReservationsForRoom(roomId);
    }

    @GetMapping("/upcoming/date/{forDate}")
    public @ResponseBody Iterable<Reservation> getUpcomingReservations(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate) {
        return reservationService.getUpcomingReservations(forDate);
    }

    @GetMapping("/upcoming/room/{roomId}/date/{forDate}")
    public @ResponseBody Iterable<Reservation> getUpcomingReservationsForRoom(@PathVariable long roomId,
                                                                              @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate) {
        return reservationService.getUpcomingReservationsForRoom(forDate, roomId);
    }

    @PostMapping("/make")
    public @ResponseBody Reservation makeReservation(@RequestBody ReservationDTO reservationDTO) {
        return reservationService.makeReservation(reservationDTO);
    }

    @DeleteMapping("/{reservationId}/cancel")
    public @ResponseBody
    ResponseEntity cancelReservation(@PathVariable long reservationId) {
        reservationService.cancelReservation(reservationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/edit")
    public @ResponseBody Reservation editReservation(@RequestBody ReservationDTO toEdit) {
        return reservationService.editReservation(toEdit);
    }

    @GetMapping("/mail")
    public void mailReservationEmails() {
        reservationService.setReservedForRooms();
    }
}
