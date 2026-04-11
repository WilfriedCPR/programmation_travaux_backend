package com.gescli.ProgrammationTravaux.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActiviteDTO {
    private String id;
    private String type;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;
    private String acteur;
    private String reference;
    private String devisId;
}
