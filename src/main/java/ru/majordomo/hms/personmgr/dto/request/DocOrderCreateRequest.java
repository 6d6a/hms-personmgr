package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import ru.majordomo.hms.personmgr.model.order.documentOrder.DeliveryType;
import ru.majordomo.hms.personmgr.model.order.documentOrder.Doc;
import ru.majordomo.hms.personmgr.model.order.documentOrder.DocOrder;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class DocOrderCreateRequest {
    @NotNull(message = "Тип доставки должен быть указан")
    private DeliveryType deliveryType;

    @NotEmpty(message = "Список документов не может быть пустым")
    private Set<Doc> docs;

    private String comment;

    public DocOrder toOrder() {
        DocOrder order = new DocOrder();
        order.setComment(comment);
        order.setDeliveryType(deliveryType);
        order.setDocs(docs);

        return order;
    }
}
