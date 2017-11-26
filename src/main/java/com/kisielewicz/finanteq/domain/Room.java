package com.kisielewicz.finanteq.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Column(name = "is_reserved", nullable = false)
    private Boolean isReserved;
}
