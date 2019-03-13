package ru.majordomo.hms.personmgr.model.order.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class SslCertificateServerType extends BaseModel {

    @NotBlank
    @ObjectId(value = SslCertificateSupplier.class)
    private String supplierId;

    @NotNull
    private Integer externalWebServerId;

    @Pattern(regexp = "^[a-z0-9 .,+'_/()!-]+$")
    @NotBlank
    @Indexed(unique = true)
    private String name;

    @Transient
    private SslCertificateSupplier supplier;
}