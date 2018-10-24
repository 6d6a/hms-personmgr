package ru.majordomo.hms.personmgr.model.order.documentOrder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryTransient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.multipart.MultipartFile;
import ru.majordomo.hms.personmgr.dto.Container;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class DocOrder extends AccountOrder {

    @NotNull(message = "Тип доставки должен быть указан")
    private DeliveryType deliveryType;

    private String documentNumber;

    @NotEmpty(message = "Адрес назначения не может быть пустым")
    private String address;

    private String comment;
    
    private String name;

    @JsonIgnore
    @Transient
    @QueryTransient
    private Container<MultipartFile[]> filesContainer;

    Set<Doc> docs = new HashSet<>();
}