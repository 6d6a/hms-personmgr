package ru.majordomo.hms.personmgr.model.order;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type")
@JsonSubTypes({
                      @JsonSubTypes.Type(value = AccountPartnerCheckoutOrder.class,
                                         name = "partnerCheckout"),
                      @JsonSubTypes.Type(value = BitrixLicenseOrder.class,
                                         name = "bitrixLicense")
              })
public abstract class AccountOrder extends ModelBelongsToPersonalAccount {
    @NotNull
    private OrderState state;
    @NotNull
    private String operator;

    @CreatedDate
    private LocalDateTime created;
    @LastModifiedDate
    private LocalDateTime updated;
    @Version
    private Long version;
}
