package ru.majordomo.hms.personmgr.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class BitrixLicenseOrderRequest {
    @NotBlank
    private String domainName;
    @NotBlank
    private String serviceId;
}
