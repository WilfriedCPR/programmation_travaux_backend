package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class AffectationRequestDTO {
    private String agentId;
    private String devisId;
    /** Autorise explicitement une affectation malgré un conflit de planning confirmé par le Chef. */
    private boolean force;
}
