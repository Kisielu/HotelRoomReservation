package com.kisielewicz.finanteq;

import com.kisielewicz.finanteq.domain.Reservation;
import com.kisielewicz.finanteq.domain.Room;
import com.kisielewicz.finanteq.dto.ReservationDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebMvc
public class FinanteqApplicationTests {

	private static final String URL = "http://localhost:8080/";
	private static final Logger LOGGER = LoggerFactory.getLogger(FinanteqApplicationTests.class);

	@Test
	public void contextLoads() {
		RestTemplate restTemplate = new RestTemplate();
		LOGGER.info("Starting story");
		LOGGER.info("Step one, getting all available rooms");
		Room[] availableRooms = restTemplate.getForObject(URL + "rooms/available/true", Room[].class);
		List<Long> availableRoomIds = Arrays.stream(availableRooms).map(Room::getId).collect(Collectors.toList());
		LOGGER.info("Got available rooms: {}", availableRoomIds);

		LOGGER.info("Looking for upcoming reservations for room: {}", availableRoomIds.get(0));
		Reservation[] upcomingReservations = restTemplate.getForObject(URL + "reservations/upcoming/room/{roomId}/date/{forDate}",
				Reservation[].class, availableRooms[0].getId(), LocalDate.now());
		LocalDate lastReservationEndDate = upcomingReservations.length > 0 ? upcomingReservations[upcomingReservations.length-1].getEndDate() : LocalDate.now();
		LOGGER.info("Found {} reservations with the last one ending at {}", upcomingReservations.length, lastReservationEndDate);

		LOGGER.info("Creating reservationDTO object");
		ReservationDTO reservationDTO = new ReservationDTO();
		reservationDTO.setStartDate(lastReservationEndDate.plusDays(1));
		reservationDTO.setEndDate(lastReservationEndDate.plusDays(2));
		reservationDTO.setMail("noreply.rekrutacja@gmail.com");
		reservationDTO.setRoomId(availableRooms[0].getId());

		Reservation[] allReservationsBefore = restTemplate.getForObject(URL + "reservations/all", Reservation[].class);
		LOGGER.info("Before making reservation, there are {} reservations total in the system", allReservationsBefore.length);

		LOGGER.info("Making reservation out of DTO object");
		Reservation madeReservation = restTemplate.postForObject(URL + "reservations/make", reservationDTO, Reservation.class);
		LOGGER.info("Made reservation with assigned id: {}", madeReservation.getId());
		assertThat(madeReservation.getStartDate()).isEqualTo(reservationDTO.getStartDate());
		assertThat(madeReservation.getEndDate()).isEqualTo(reservationDTO.getEndDate());
		assertThat(madeReservation.getMail()).isEqualTo(reservationDTO.getMail());
		assertThat(madeReservation.getRoom().getId()).isEqualTo(reservationDTO.getRoomId());

		Reservation[] allReservationsAfter = restTemplate.getForObject(URL + "reservations/all", Reservation[].class);
		LOGGER.info("After making reservation, there are {} reservations total in the system", allReservationsAfter.length);
		assertThat(Arrays.asList(allReservationsAfter).contains(madeReservation)).isTrue();

		LOGGER.info("Deleting reservation made earlier");
		restTemplate.delete(URL + "reservations/cancel/{reservationId}", madeReservation.getId());
		Reservation[] allReservationsAfterDelete = restTemplate.getForObject(URL + "reservations/all", Reservation[].class);
		LOGGER.info("After deleting reservation, there are {} reservations total in the system", allReservationsAfterDelete.length);
		assertThat(Arrays.asList(allReservationsAfterDelete).contains(madeReservation)).isFalse();
	}

}
