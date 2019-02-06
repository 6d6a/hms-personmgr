package ru.majordomo.hms.personmgr.model.promoActions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotEmpty;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.ValidPhone;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;

import java.time.LocalDateTime;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class GoogleAdsRequest extends ModelBelongsToPersonalAccount {
    @ValidEmail
    @NotEmpty
    private String email;

    @NotEmpty
    private String name;

    @NotEmpty
    @ValidPhone
    private String phone;

    private LocalDateTime created;

    @NotEmpty
    Set<String> domains;
}
