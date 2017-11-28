package com.kisielewicz.finanteq.repository;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.domain.Room;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {

    Iterable<Reservation> findAllByRoom(Room room);

    Iterable<Reservation> findAllByStartDateAfterAndRoom(LocalDate after, Room room);

    Iterable<Reservation> findAllByStartDateAfter(LocalDate after);

    Iterable<Reservation> findAllByRoomAndStartDateBeforeAndEndDateAfter(Room room, LocalDate localEndDate, LocalDate localStartDate);

}
