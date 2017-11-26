package com.kisielewicz.finanteq.repository;

import com.kisielewicz.finanteq.domain.Room;
import org.springframework.data.repository.CrudRepository;

public interface RoomRepository extends CrudRepository<Room, Long> {

    Iterable<Room> findAllByIsReserved(Boolean isReserved);
}
