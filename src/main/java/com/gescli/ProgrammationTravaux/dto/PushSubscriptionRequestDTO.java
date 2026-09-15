package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushSubscriptionRequestDTO {
    @NotBlank private String endpoint;
    private String p256dh;
    private String auth;
}
