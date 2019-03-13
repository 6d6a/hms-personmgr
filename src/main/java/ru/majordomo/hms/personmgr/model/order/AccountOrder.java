package ru.majordomo.hms.personmgr.model.order;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateOrder;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AccountPartnerCheckoutOrder.class, name = "partnerCheckout"),
        @JsonSubTypes.Type(value = BitrixLicenseOrder.class, name = "bitrixLicense"),
        @JsonSubTypes.Type(value = SslCertificateOrder.class, name = "sslCertificateOrder")
})
public abstract class AccountOrder extends VersionedModelBelongsToPersonalAccount {
    @JsonView(Views.Public.class)
    @NotNull
    private OrderState state;

    @JsonView(Views.Internal.class)
    @NotNull
    private String operator;

    @JsonView(Views.Internal.class)
    @CreatedDate
    private LocalDateTime created;

    @JsonView(Views.Public.class)
    @LastModifiedDate
    private LocalDateTime updated;
}
