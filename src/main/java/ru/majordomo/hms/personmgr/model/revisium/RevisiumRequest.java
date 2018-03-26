package ru.majordomo.hms.personmgr.model.revisium;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class RevisiumRequest extends ModelBelongsToPersonalAccount {

    @NotNull
    @ObjectId(RevisiumRequestService.class)
    private String revisiumRequestServiceId;

    private String result;

    private String requestId;

    private LocalDateTime created;

    private Boolean successCheck = null;

    private Boolean successGetResult = null;
}
