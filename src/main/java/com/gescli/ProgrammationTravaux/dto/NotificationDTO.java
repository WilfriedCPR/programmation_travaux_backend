package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private String id;
    private String titre;
    private String message;
    private String type;
    private String lien;
    private LocalDateTime dateCreation;
    private boolean lue;
}
