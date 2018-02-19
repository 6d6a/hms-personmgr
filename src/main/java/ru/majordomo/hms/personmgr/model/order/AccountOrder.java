package ru.majordomo.hms.personmgr.model.order;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
    @NotNull
    private LocalDateTime created;
    @NotNull
    private LocalDateTime updated;
}
