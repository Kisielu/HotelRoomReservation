package com.kisielewicz.finanteq.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationDTO {

    private Long reservationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long roomId;
    private String mail;
}
