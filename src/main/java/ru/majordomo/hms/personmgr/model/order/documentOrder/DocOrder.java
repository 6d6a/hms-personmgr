package ru.majordomo.hms.personmgr.model.order.documentOrder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class DocOrder extends AccountOrder {

    @NotNull(message = "Тип доставки должен быть указан")
    private DeliveryType deliveryType;

    private String documentNumber;

    @NotEmpty(message = "Список документов не может быть пустым")
    private Set<Doc> docs;

    @NotEmpty(message = "Адрес назначения не может быть пустым")
    private String address;

    private String comment;
}