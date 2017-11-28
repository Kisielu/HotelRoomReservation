package com.kisielewicz.finanteq.service

import com.kisielewicz.finanteq.domain.Reservation
import com.kisielewicz.finanteq.domain.Room
import com.kisielewicz.finanteq.dto.ReservationDTO
import com.kisielewicz.finanteq.exceptions.ConflictException
import com.kisielewicz.finanteq.exceptions.NotFoundException
import com.kisielewicz.finanteq.repository.ReservationRepository
import com.kisielewicz.finanteq.repository.RoomRepository
import spock.lang.Specification

import java.time.LocalDate

class ReservationServiceSpecTest extends Specification {

    private final ReservationRepository reservationRepository = Mock(ReservationRepository)
    private final RoomRepository roomRepository = Mock(RoomRepository)

    private final ReservationService reservationService = new ReservationService(reservationRepository, roomRepository)

    def "should return all reservations made"() {
        given:
        Iterable<Reservation> reservations = new ArrayList<>()
        for (int i = 0; i < 5; i++) {
            reservations.add(new Reservation().setId(i))
        }
        when:
        Iterable<Reservation> result = reservationService.getAllReservations()
        then:
        1*reservationRepository.findAll() >> reservations
        result == reservations
        result.size() == 5
    }

    def "should return empty list of reservations"() {
        given:
        Iterable<Reservation> reservations = new ArrayList<>()
        when:
        Iterable<Reservation> result = reservationService.getAllReservations()
        then:
        1*reservationRepository.findAll() >> reservations
        result == reservations
        result.size() == 0
    }

    def "should return reservations by id"() {
        given:
        int res = 3
        Reservation reservation
        Iterable<Reservation> reservations = new ArrayList<>()
        for (int i = 0; i < 5; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservations.add(reservation)
        }
        when:
        Reservation result = reservationService.getReservation(res)
        then:
        1*reservationRepository.findOne(res) >> reservations[res]
        reservations.contains(result)
        result.id == res
    }

    def "should return empty reservation"() {
        given:
        int res = 3
        when:
        Reservation result = reservationService.getReservation(3)
        then:
        1*reservationRepository.findOne(res) >> null
        result == null
    }

    def "should return all for room"() {
        given:
        int res = 1
        Reservation reservation
        Room room = new Room()
        room.setId(res)
        Iterable<Reservation> reservations = new ArrayList<>()
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservations.add(reservation)
        }
        when:
        Iterable<Reservation> result = reservationService.getAllReservationsForRoom(res)
        then:
        1*roomRepository.findOne(res) >> room
        1*reservationRepository.findAllByRoom(room) >> reservations
        result.size() == 2
        result[1].room.id == res
    }

    def "should return empty reservation when no reservations for room"() {
        given:
        int res = 1
        Reservation reservation
        Room room = new Room()
        room.setId(res)
        Iterable<Reservation> reservations = new ArrayList<>()
        for (int i = 2; i < 4; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservations.add(reservation)
        }
        when:
        Iterable<Reservation> result = reservationService.getAllReservationsForRoom(res)
        then:
        1*roomRepository.findOne(res) >> room
        1*reservationRepository.findAllByRoom(room) >> new ArrayList<>()
        result.size() == 0
    }

    def "should throw NotFoundException when no room found"() {
        given:
        int res = 1
        when:
        Iterable<Reservation> result = reservationService.getAllReservationsForRoom(res)
        then:
        NotFoundException e = thrown()
        1*roomRepository.findOne(res) >> null
        0*reservationRepository.findAllByRoom(_)
        e.getMessage() == "No room by id:1 found."
    }

    def "should find all reservations with date after some date"() {
        given:
        int res = 1
        Reservation reservation
        Room room = new Room()
        room.setId(res)
        Iterable<Reservation> reservations = new ArrayList<>()
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+1))
            reservations.add(reservation)
        }
        when:
        Iterable<Reservation> result = reservationService.getUpcomingReservations(LocalDate.now())
        then:
        1*reservationRepository.findAllByStartDateAfter(LocalDate.now()) >> reservations
        result.size() == 2
        result[0].startDate == reservations[0].startDate
    }

    def "should find no reservations for date specified"() {
        given:
        when:
        Iterable<Reservation> result = reservationService.getUpcomingReservations(LocalDate.now())
        then:
        1*reservationRepository.findAllByStartDateAfter(LocalDate.now()) >> new ArrayList<>()
        result.size() == 0
    }

    def "should successfully cancel reservation"() {
        given:
        int reservationId = 1
        Room room = new Room()
        room.setId(7)
        room.setIsReserved(false)
        Reservation reservation = new Reservation()
        reservation.setId(1)
        reservation.setStartDate(LocalDate.now().plusDays(2))
        reservation.setEndDate(LocalDate.now().plusDays(4))
        reservation.setMail("test@test.com")
        reservation.setRoom(room)
        when:
        reservationService.cancelReservation(reservationId)
        then:
        1*reservationRepository.exists(reservationId) >> true
        1*reservationRepository.findOne(reservationId) >> reservation
        1*reservationRepository.delete(reservationId)
    }

    def "should successfully cancel ongoing reservation and set room to not reserved"() {
        given:
        int reservationId = 1
        Room room = new Room()
        room.setId(7)
        room.setIsReserved(true)
        Reservation reservation = new Reservation()
        reservation.setId(1)
        reservation.setStartDate(LocalDate.now().minusDays(2))
        reservation.setEndDate(LocalDate.now().plusDays(4))
        reservation.setMail("test@test.com")
        reservation.setRoom(room)


        Room changedRoom = new Room()
        changedRoom.setId(7)
        changedRoom.setIsReserved(false)
        when:
        reservationService.cancelReservation(reservationId)
        then:
        1*reservationRepository.exists(reservationId) >> true
        1*reservationRepository.findOne(reservationId) >> reservation
        1*roomRepository.save(changedRoom) >> changedRoom
        1*reservationRepository.delete(reservationId)
    }

    def "should throw NotFoundException when no reservation found"() {
        given:
        int reservationId = 1

        when:
        reservationService.cancelReservation(reservationId)
        then:
        1*reservationRepository.exists(reservationId) >> false
        NotFoundException e = thrown()
        e.getMessage() == "No reservation by id:1 found."
    }

    def "should successfully create reservation"() {
        given:
        int roomId = 5
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setStartDate(LocalDate.now().plusDays(2))
        reservationDTO.setEndDate(LocalDate.now().plusDays(4))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation shouldResult = new Reservation()
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        Reservation result = reservationService.makeReservation(reservationDTO)
        then:
        1*roomRepository.findOne(roomId) >> room
        1*reservationRepository.findAllByRoomAndStartDateBeforeAndEndDateAfter(
                room, reservationDTO.getEndDate(), reservationDTO.getStartDate()) >> new ArrayList<>()
        1*reservationRepository.save(_) >> shouldResult
        result.getMail() == reservationDTO.getMail()
    }

    def "should throw NotFoundException when no room present"() {
        given:
        int roomId = 5
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setStartDate(LocalDate.now().plusDays(2))
        reservationDTO.setEndDate(LocalDate.now().plusDays(4))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation shouldResult = new Reservation()
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        Reservation result = reservationService.makeReservation(reservationDTO)
        then:
        1*roomRepository.findOne(roomId) >> null
        NotFoundException e = thrown()
        e.getMessage() == "No room by id:" + roomId + " found."
        0*reservationRepository.findAllByStartDateAfterAndRoom(LocalDate.now().minusDays(1), room) >> reservations
        0*reservationRepository.save(_) >> shouldResult
    }

    def "should throw ConflictException with wrong date range"() {
        given:
        int roomId = 5
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setStartDate(LocalDate.now().plusDays(4))
        reservationDTO.setEndDate(LocalDate.now().plusDays(2))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation shouldResult = new Reservation()
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        reservationService.makeReservation(reservationDTO)
        then:
        1*roomRepository.findOne(roomId) >> room
        ConflictException e = thrown()
        e.getMessage() == "Date range input is wrong."
        0*reservationRepository.findAllByStartDateAfterAndRoom(LocalDate.now().minusDays(1), room) >> reservations
        0*reservationRepository.save(_) >> shouldResult
    }

    def "should throw ConflictException with start date too early"() {
        given:
        int roomId = 5
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setStartDate(LocalDate.now().minusDays(4))
        reservationDTO.setEndDate(LocalDate.now().plusDays(2))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation shouldResult = new Reservation()
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        reservationService.makeReservation(reservationDTO)
        then:
        1*roomRepository.findOne(roomId) >> room
        ConflictException e = thrown()
        e.getMessage() == "Can't make reservation with start date before today."
        0*reservationRepository.findAllByStartDateAfterAndRoom(LocalDate.now().minusDays(1), room) >> reservations
        0*reservationRepository.save(_) >> shouldResult
    }

    def "should throw ConflictException with some reservation already done for date" () {
        given:
        int roomId = 5
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setStartDate(LocalDate.now().plusDays(8))
        reservationDTO.setEndDate(LocalDate.now().plusDays(20))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation shouldResult = new Reservation()
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        reservationService.makeReservation(reservationDTO)
        then:
        1*roomRepository.findOne(roomId) >> room
        1*reservationRepository.findAllByRoomAndStartDateBeforeAndEndDateAfter(
                room, reservationDTO.getEndDate(), reservationDTO.getStartDate()) >> reservations
        ConflictException e = thrown()
        e.getMessage() == "Reservation for this room is already created for desired date range."
        0*reservationRepository.save(_) >> shouldResult
    }

    def "should successfully edit reservation"() {
        given:
        int roomId = 5
        int reservationId = 1
        Room room = new Room()
        room.setId(roomId)
        room.setIsReserved(false)
        ReservationDTO reservationDTO = new ReservationDTO()
        reservationDTO.setReservationId(reservationId)
        reservationDTO.setStartDate(LocalDate.now().plusDays(2))
        reservationDTO.setEndDate(LocalDate.now().plusDays(4))
        reservationDTO.setMail("test@test.com")
        reservationDTO.setRoomId(roomId)

        Reservation oldReservation = new Reservation()
        oldReservation.setId(reservationId)
        oldReservation.setStartDate(LocalDate.now().plusDays(17))
        oldReservation.setEndDate(LocalDate.now().plusDays(26))
        oldReservation.setMail("old@old.com")
        oldReservation.setRoom(room)

        Reservation shouldResult = new Reservation()
        shouldResult.setId(reservationId)
        shouldResult.setStartDate(LocalDate.now().plusDays(2))
        shouldResult.setEndDate(LocalDate.now().plusDays(4))
        shouldResult.setMail("test@test.com")
        shouldResult.setRoom(room)

        Iterable<Reservation> reservations = new ArrayList<>()
        Reservation reservation
        for (int i = 0; i < 2; i++) {
            reservation = new Reservation()
            reservation.setId(i)
            reservation.setRoom(room)
            reservation.setStartDate(LocalDate.now().plusDays(i+10))
            reservation.setEndDate(LocalDate.now().plusDays(i+11))
            reservations.add(reservation)
        }
        when:
        Reservation result = reservationService.editReservation(reservationDTO)
        then:
        1*reservationRepository.findOne(reservationId) >> oldReservation
        1*roomRepository.findOne(roomId) >> room
        1*reservationRepository.findAllByRoomAndStartDateBeforeAndEndDateAfter(
                room, reservationDTO.getEndDate(), reservationDTO.getStartDate()) >> new ArrayList<>()
        1*reservationRepository.save(_) >> shouldResult
        result.getMail() == reservationDTO.getMail()
    }
}
