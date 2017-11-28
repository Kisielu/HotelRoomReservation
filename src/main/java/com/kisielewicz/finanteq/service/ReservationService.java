package com.kisielewicz.finanteq.service;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.dto.ReservationDTO;
import com.kisielewicz.finanteq.exceptions.ConflictException;
import com.kisielewicz.finanteq.exceptions.NotFoundException;
import com.kisielewicz.finanteq.repository.ReservationRepository;
import com.kisielewicz.finanteq.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.kisielewicz.finanteq.helpers.Mailer.mail;
import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private static final String RESERVATION_ALREADY_DONE_IN_DATE_RANGE = "Reservation for this room is already created for desired date range.";
    private static final String WRONG_DATE_RANGE = "Date range input is wrong.";
    private static final String ROOM_NOT_FOUND = "No room by id:%s found.";
    private static final String RESERVATION_NOT_FOUND = "No reservation by id:%s found.";
    private static final String RESERVATION_START_DATE_TOO_EARLY = "Can't make reservation with start date before today.";

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getAllReservations() {
        LOGGER.info("Getting all reservations");
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(long reservationId) {
        LOGGER.info("Getting reservation for id: {}", reservationId);
        return reservationRepository.findOne(reservationId);
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getAllReservationsForRoom(long roomId) {
        LOGGER.info("Getting all reservations for room for id: {}", roomId);
        return reservationRepository.findAllByRoom_Id(roomId);
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getUpcomingReservations(LocalDate localDate) {
        LOGGER.info("Getting all reservations for start date after: {}", localDate);
        return reservationRepository.findAllByStartDateAfter(localDate.minusDays(1));
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getUpcomingReservationsForRoom(LocalDate localDate, long roomId) {
        LOGGER.info("Getting all reservations for room for id: {} and start date after: {}",roomId , localDate);
        return reservationRepository.findAllByStartDateAfterAndRoom_Id(localDate.minusDays(1), roomId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelReservation(long reservationId) {
        LOGGER.info("Canceling reservation for id: {}", reservationId);
        if (reservationExists(reservationId)) {
            isReservationStartingToday(reservationId);
            reservationRepository.delete(reservationId);
            LOGGER.info("Successfully cancelled reservation for id: {}", reservationId);
        } else {
            LOGGER.info("Unable to find reservation for id: {}, throwing 404 exception", reservationId);
            throw new NotFoundException(String.format(RESERVATION_NOT_FOUND, reservationId));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reservation makeReservation(ReservationDTO reservationDTO) {
        LOGGER.info("Making reservation for room: {}, startDate: {}, endDate: {} and mail: {}",
                reservationDTO.getRoomId(), reservationDTO.getStartDate(),
                reservationDTO.getEndDate(), reservationDTO.getMail());
        validateReservationDTO(reservationDTO);
        Reservation reservation = new Reservation();

        return parseDTOToReservationAndSave(reservationDTO, reservation);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reservation editReservation(ReservationDTO reservationDTO) {
        LOGGER.info("Editing reservation for id: {}, setting new room: {}, startDate: {}, endDate: {} and mail: {}",
                reservationDTO.getRoomId(), reservationDTO.getRoomId(), reservationDTO.getStartDate(),
                reservationDTO.getEndDate(), reservationDTO.getMail());

        validateReservationDTO(reservationDTO);
        Reservation reservation = reservationRepository.findOne(reservationDTO.getReservationId());

        return parseDTOToReservationAndSave(reservationDTO, reservation);
    }

    @Scheduled(cron = "0 0 14 * * *")
    @Transactional(readOnly = true)
    public void setReservedForRooms() {
        LOGGER.info("Starting mailing about reservations which start tomorrow.");
        Iterable<Reservation> reservations = reservationRepository.findAllByStartDate(LocalDate.now().plusDays(1));
        for(Reservation reservation : reservations) {
            mail(reservation.getMail());
        }
        LOGGER.info("Finished mailing about reservations which start tomorrow.");
    }

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reviseRoomsAvailability() {
        LOGGER.info("Revising room availability");
        Iterable<Reservation> endingReservations = reservationRepository.findAllByEndDate(LocalDate.now());
        for(Reservation reservation : endingReservations) {
            reservation.getRoom().setIsReserved(false);
            roomRepository.save(reservation.getRoom());
        }
        Iterable<Reservation> startingReservations = reservationRepository.findAllByStartDate(LocalDate.now());
        for(Reservation reservation : startingReservations) {
            reservation.getRoom().setIsReserved(true);
            roomRepository.save(reservation.getRoom());
        }
        LOGGER.info("Ended revising room availability");
    }

    private Reservation parseDTOToReservationAndSave(ReservationDTO reservationDTO, Reservation reservation) {
        LOGGER.info("Parsing reservationDTO to entity");
        parametersFromDTO(reservation, reservationDTO);

        if (isReservationPossibleForInput(reservation)) {
            LOGGER.info("Reservation is possible - saving reservation");
            return saveReservation(reservation);
        } else {
            LOGGER.info("Reservation is not possible - throwing ConflictException");
            throw new ConflictException(RESERVATION_ALREADY_DONE_IN_DATE_RANGE);
        }
    }

    private void validateReservationDTO(ReservationDTO reservationDTO) {
        LOGGER.info("Validating reservationDTO");

        if (!reservationDTO.getStartDate().isBefore(reservationDTO.getEndDate())) {
            LOGGER.info("StartDate {} is after endDate {}, throwing ConflictException", reservationDTO.getStartDate(),
                    reservationDTO.getEndDate());
            throw new ConflictException(WRONG_DATE_RANGE);
        }

        if (DAYS.between(LocalDate.now(), reservationDTO.getStartDate()) < 0) {
            LOGGER.info("StartDate {} is before today, throwing ConflictException", reservationDTO.getStartDate());
            throw new ConflictException(RESERVATION_START_DATE_TOO_EARLY);
        }
    }

    private boolean isReservationPossibleForInput(Reservation reservation) {
        LOGGER.info("Checking if reservation is possible - " +
                "if there is no other reservations in the time period for the room specified");
        Iterable<Reservation> reservationsForRoomAndDate =
                reservationRepository.findAllByRoomAndStartDateBeforeAndEndDateAfter(
                        reservation.getRoom(), reservation.getEndDate(), reservation.getStartDate());

        return !reservationsForRoomAndDate.iterator().hasNext();
    }

    private void isReservationStartingToday(long reservationId) {
        LOGGER.info("Checking if reservation pending cancellation starts today", reservationId);
        Reservation reservation = reservationRepository.findOne(reservationId);
        if (LocalDate.now().isAfter(reservation.getStartDate().minusDays(1)) && LocalDate.now().isBefore(reservation.getEndDate().plusDays(1))) {
            LOGGER.info("Reservation pending cancellation starts today, setting room of cancelled reservation to available");
            reservation.getRoom().setIsReserved(false);
            roomRepository.save(reservation.getRoom());
        }
    }

    private Reservation saveReservation(Reservation reservation) {
        try {
            LOGGER.info("Attempting to save reservation");
            return reservationRepository.save(reservation);
        } catch (CannotAcquireLockException e) {
            LOGGER.info("Failed to save reservation because of lock on database, " +
                    "some thread already saved the data with this date range.");
            throw new ConflictException(RESERVATION_ALREADY_DONE_IN_DATE_RANGE);
        }
    }

    private void parametersFromDTO(Reservation reservation, ReservationDTO reservationDTO) {
        LOGGER.info("Starting the parse from DTO to entity");
        reservation.setStartDate(reservationDTO.getStartDate());
        reservation.setEndDate(reservationDTO.getEndDate());
        reservation.setMail(reservationDTO.getMail());

        LOGGER.info("Looking for a room for id: {}", reservationDTO.getRoomId());
        Room room = roomRepository.findOne(reservationDTO.getRoomId());
        if (room != null) {
            LOGGER.info("Successfully found a room, setting");
            reservation.setRoom(room);
        } else {
            LOGGER.info("Didn't find a room for id: {}, returning 404 exception.", reservationDTO.getRoomId());
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, reservationDTO.getRoomId()));
        }
    }

    private boolean reservationExists(long reservationId) {
        return reservationRepository.exists(reservationId);
    }
}
