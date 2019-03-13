package ru.majordomo.hms.personmgr.model.order.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Document
@EqualsAndHashCode(callSuper = true)
@Data
public class SslCertificateApproverEmail extends BaseModel {

    @NotBlank
    @Indexed(unique = true)
    @Pattern(regexp = "^[a-z0-9.+-]+$")
    private String name;
}