package ru.majordomo.hms.personmgr.model.order.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.*;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class SslCertificateProduct extends BaseModel {

    @ObjectId(SslCertificateSupplier.class)
    private String supplierId;

    @NotBlank
    private String externalProductId;

    @Pattern(regexp = "^[a-z0-9() -]+$")
    @Size(max = 64)
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private boolean wildcard;

    private boolean onlyOrganizations;

    private boolean unlimited;

    private boolean multidomain;

    @Range(min = 1, max = 10)
    private int period;

    @ObjectId(value = PaymentService.class)
    @NotBlank
    private String serviceId;

    @ObjectId(value = PaymentService.class)
    private String additionalDomainServiceId;

    @Transient
    private PaymentService service;

    @Transient
    private PaymentService additionalDomainService;

    @Transient
    private SslCertificateSupplier supplier;

    private boolean active;
}
