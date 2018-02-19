package ru.majordomo.hms.personmgr.model.order;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

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
    private String documentNumber;
}
