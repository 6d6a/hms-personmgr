package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class DocumentOrder extends ModelBelongsToPersonalAccount {

    @CreatedDate
    @Indexed
    private LocalDateTime createdDateTime;

    private List<DocumentType> documentTypes = new ArrayList<>();

    /**
     * Произвольный набор параметров. Например day, mount, year, phone, postalAddress, ustava, urfio и много, много других
     */
    private Map<String, String> params = new HashMap<>();

    private List<String> domainIds = new ArrayList<>();

    private Boolean checked = false;

    private Boolean agreement = false;

    private Boolean paid = false;

    private Map<String, String> errors = new HashMap<>();

    private Boolean send = false;

    private String postalAddress;

    /**
     * номер списания. finansier.BillingOperation.documentNumber
     * null если списаний не проводилось
     */
    @Nullable
    private String documentNumber;

    /**
     * Доступное количество бесплатных заказов документов по почте
     * null или 0 если бесплатных заказов нет.
     * Нужно для frontend при проверке доступности заказа
     */
    @Nullable
    @Transient
    private Integer freeOrdersAvailable;
}
