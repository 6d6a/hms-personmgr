package ru.majordomo.hms.personmgr.model.order;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class BitrixLicenseOrder extends AccountOrder {
    @NotBlank
    private String domainName;
    @NotBlank
    private String serviceId;
    @Transient
    private String serviceName;
    private String documentNumber;
}
