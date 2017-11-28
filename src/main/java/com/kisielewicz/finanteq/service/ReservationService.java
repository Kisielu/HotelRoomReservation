package com.kisielewicz.finanteq.service;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.dto.ReservationDTO;
import com.kisielewicz.finanteq.exceptions.ConflictException;
import com.kisielewicz.finanteq.exceptions.NotFoundException;
import com.kisielewicz.finanteq.repository.ReservationRepository;
import com.kisielewicz.finanteq.repository.RoomRepository;
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
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(long reservationId) {
        return reservationRepository.findOne(reservationId);
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getAllReservationsForRoom(long roomId) {
        Room room = roomRepository.findOne(roomId);
        if (room != null) {
            return reservationRepository.findAllByRoom(room);
        } else {
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, roomId));
        }
    }

    @Transactional(readOnly = true)
    public Iterable<Reservation> getUpcomingReservations(LocalDate localDate) {
        return reservationRepository.findAllByStartDateAfter(localDate);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelReservation(long reservationId) {
        if (reservationExists(reservationId)) {
            isReservationOngoing(reservationId);
            reservationRepository.delete(reservationId);
        } else {
            throw new NotFoundException(String.format(RESERVATION_NOT_FOUND, reservationId));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reservation makeReservation(ReservationDTO reservationDTO) {

        Reservation reservation = new Reservation();

        return parseDTOToReservationAndSave(reservationDTO, reservation);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reservation editReservation(ReservationDTO reservationDTO) {

        Reservation reservation = reservationRepository.findOne(reservationDTO.getReservationId());

        return parseDTOToReservationAndSave(reservationDTO, reservation);
    }

    @Scheduled(cron = "0 0 14 * * *")
    @Transactional(readOnly = true)
    public void setReservedForRooms() {
        Iterable<Reservation> reservations = reservationRepository.findAll();
        reservations.forEach(this::setReservedOnRoom);
        reservations.forEach(this::sendEmails);
    }

    private void setReservedOnRoom(Reservation reservation) {
        if (isWithinRange(reservation)) {
            reservation.getRoom().setIsReserved(true);
        } else {
            reservation.getRoom().setIsReserved(false);
        }
        roomRepository.save(reservation.getRoom());
    }

    private Reservation parseDTOToReservationAndSave(ReservationDTO reservationDTO, Reservation reservation) {
        parametersFromDTO(reservation, reservationDTO);

        if (!reservation.getStartDate().isBefore(reservation.getEndDate())) {
            throw new ConflictException(WRONG_DATE_RANGE);
        }

        if (DAYS.between(LocalDate.now(), reservation.getStartDate()) >= 0) {
            if (isReservationPossibleForInput(reservation)) {
                return saveReservation(reservation);
            } else {
                throw new ConflictException(RESERVATION_ALREADY_DONE_IN_DATE_RANGE);
            }
        } else {
            throw new ConflictException(RESERVATION_START_DATE_TOO_EARLY);
        }
    }

    private boolean isReservationPossibleForInput(Reservation reservation) {
        Iterable<Reservation> reservationsForRoomAndDate =
                reservationRepository.findAllByRoomAndStartDateBeforeAndEndDateAfter(
                        reservation.getRoom(), reservation.getEndDate(), reservation.getStartDate());

        return !reservationsForRoomAndDate.iterator().hasNext();
    }

    private void isReservationOngoing(long reservationId) {
        Reservation reservation = reservationRepository.findOne(reservationId);
        if (LocalDate.now().isAfter(reservation.getStartDate().minusDays(1)) && LocalDate.now().isBefore(reservation.getEndDate().plusDays(1))) {
            reservation.getRoom().setIsReserved(false);
            roomRepository.save(reservation.getRoom());
        }
    }

    private Reservation saveReservation(Reservation reservation) {
        try {
            return reservationRepository.save(reservation);
        } catch (CannotAcquireLockException e) {
            throw new ConflictException(RESERVATION_ALREADY_DONE_IN_DATE_RANGE);
        }
    }

    private void parametersFromDTO(Reservation reservation, ReservationDTO reservationDTO) {
        reservation.setStartDate(reservationDTO.getStartDate());
        reservation.setEndDate(reservationDTO.getEndDate());
        reservation.setMail(reservationDTO.getMail());

        Room room = roomRepository.findOne(reservationDTO.getRoomId());
        if (room != null) {
            reservation.setRoom(room);
        } else {
            throw new NotFoundException(String.format(ROOM_NOT_FOUND, reservationDTO.getRoomId()));
        }
    }

    private boolean reservationExists(long reservationId) {
        return reservationRepository.exists(reservationId);
    }

    private boolean isWithinRange(Reservation reservation) {
        return LocalDate.now().isAfter(reservation.getStartDate().minusDays(1)) && LocalDate.now().isBefore(reservation.getEndDate());
    }

    private void sendEmails(Reservation reservation) {
        if (DAYS.between(LocalDate.now(), reservation.getStartDate()) == 1 && LocalDate.now().isBefore(reservation.getStartDate())) {
            mail(reservation.getMail());
        }
    }
}
