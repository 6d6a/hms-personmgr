package ru.majordomo.hms.personmgr.model.order;

import com.fasterxml.jackson.annotation.JsonView;
import javax.validation.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.Views;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class BitrixLicenseOrder extends AccountOrder {
    @JsonView(Views.Public.class)
    @NotBlank
    private String domainName;

    @JsonView(Views.Public.class)
    @NotBlank
    private String serviceId;

    @JsonView(Views.Public.class)
    @Transient
    private String serviceName;

    private String documentNumber;
    private LicenseType type;
    private String previousOrderId;
    private String comment;

    @Transient
    private BitrixLicenseOrder previousOrder;

    public enum LicenseType {NEW, PROLONG}
}
