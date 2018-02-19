package ru.majordomo.hms.personmgr.dto;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Data;

@Data
public class BitrixLicenseOrderRequest {
    @NotBlank
    private String domainName;
    @NotBlank
    private String serviceId;
}
