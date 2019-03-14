package ru.majordomo.hms.personmgr.model.order.ssl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryTransient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;
import ru.majordomo.hms.personmgr.validation.DomainName;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.personmgr.validation.ValidSSLOrder;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@ValidSSLOrder
public class SslCertificateOrder extends AccountOrder {
    public enum Type {
        NEW, RENEW
    }

    @ObjectId(value = SslCertificateProduct.class)
    @NotBlank
    private String productId;

    @ObjectId(value = SslCertificateServerType.class)
    @NotBlank
    private String serverTypeId;

    @ObjectId(value = SslCertificateApproverEmail.class)
    @NotBlank
    private String approverEmailId;

    @NotNull
    private Boolean isOrganization;

    @DomainName
    @Length(min = 4, max = 255)
    private String domainName;

    private Integer externalOrderId;

    @JsonIgnore
    @Size(min = 512, max = 4096)
    private String csr;

    private ExternalState externalState;

    private LocalDate validFrom;
    private LocalDate validTo;

    private Type orderType = Type.NEW;

    @Pattern(regexp = "^[a-zA-Z\'-]+$")
    @Size(min = 2, max = 64)
    @NotBlank
    private String firstName;

    @Pattern(regexp = "^[a-zA-Z\'-]+$")
    @Size(min = 2, max = 64)
    @NotBlank
    private String lastName;

    @Pattern(regexp = "^[a-zA-Z -]+$")
    @Size(min = 2, max = 64)
    @NotBlank
    private String title;

    @Pattern(regexp = "^[a-zA-Z0-9 .-]+$")
    @Size(max = 64)
    private String organizationName;

    @Pattern(regexp = "^[a-zA-Z -]+$")
    @Size(max = 64)
    private String division;

    @Size(min = 4, max = 128)
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+[0-9]{1,5} [0-9]{3,5} [0-9]{3,7}$")
    @Size(max = 20)
    private String phone;

    @Pattern(regexp = "^\\+[0-9]{1,5} [0-9]{3,5} [0-9]{3,7}$")
    @Size(max = 20)
    private String fax;

    @NotBlank
    @ObjectId(Country.class)
    private String countryId;

    @NotBlank
    @Pattern(regexp = "^[0-9a-z]+$")
    private String postalCode;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9A-Z -]+$")
    @Size(max = 64)
    private String city;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9 /-]+$")
    @Size(max = 64)
    private String region;

    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9 .,-]+$")
    @NotBlank
    private String addressLineFirst;

    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9 .,-]+$")
    private String addressLineSecond;

    private String documentNumber;

    private List<String> chain;

    private String key;

    @Transient
    @QueryTransient
    private SslCertificateServerType serverType;

    @Transient
    @QueryTransient
    private SslCertificateProduct product;

    @Transient
    @QueryTransient
    private SslCertificateApproverEmail approverEmail;

    @Transient
    @QueryTransient
    private Country country;
}
