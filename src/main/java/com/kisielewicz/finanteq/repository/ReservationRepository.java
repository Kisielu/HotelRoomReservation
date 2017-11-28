package com.kisielewicz.finanteq.repository;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.domain.Room;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface ReservationRepository extends CrudRepository<Reservation, Long> {

    Iterable<Reservation> findAllByStartDate(LocalDate localDate);

    Iterable<Reservation> findAllByEndDate(LocalDate localDate);

    Iterable<Reservation> findAllByStartDateAfter(LocalDate after);

    Iterable<Reservation> findAllByRoomAndStartDateBeforeAndEndDateAfter(Room room, LocalDate localEndDate, LocalDate localStartDate);

    Iterable<Reservation> findAllByStartDateAfterAndRoom_Id(LocalDate after, long roomId);

    Iterable<Reservation> findAllByRoom_Id(long roomId);

}
