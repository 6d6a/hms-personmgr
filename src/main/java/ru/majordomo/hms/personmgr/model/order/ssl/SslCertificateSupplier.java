package ru.majordomo.hms.personmgr.model.order.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class SslCertificateSupplier extends BaseModel {
    @Indexed(unique = true)
    @Pattern(regexp = "^[a-z]+$")
    @NotBlank
    private String name;
}
