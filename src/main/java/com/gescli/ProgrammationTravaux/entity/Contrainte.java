package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tr_contrainte")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Contrainte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "created_date") 
    private LocalDateTime createdDate;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "tr_contrainte_libelle")
    private String libelle;
}