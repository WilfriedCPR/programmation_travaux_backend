package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class OperationDTO {
    private String id;
    private String typeOperation;
    private String agentId;
    private String demandeMaterielId;
}
