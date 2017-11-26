package com.kisielewicz.finanteq.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Column(name = "reservation_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "reservation_end_date", nullable = false)
    private LocalDate endDate;

    @OneToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "mail", nullable = false)
    private String mail;
}
