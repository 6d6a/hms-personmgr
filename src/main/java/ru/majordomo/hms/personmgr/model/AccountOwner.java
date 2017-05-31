package ru.majordomo.hms.personmgr.model;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;
import ru.majordomo.hms.rc.user.resources.validation.ValidPhone;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Document
@UniquePersonalAccountIdModel(AccountOwner.class)
public class AccountOwner extends VersionedModelBelongsToPersonalAccount {
    @NotBlank
    private String name;

    @Valid
    private List<@ValidPhone String> phoneNumbers = new ArrayList<>();

    @NotEmpty(message = "Должен быть указан хотя бы 1 email адрес")
    @Valid
    private List<@ValidEmail String> emailAddresses = new ArrayList<>();

    @NotBlank
    private String country;

    @NotNull
    @Valid
    private Address postalAddress;
}
