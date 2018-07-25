package ru.majordomo.hms.personmgr.dto.revisium;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class RevisiumRequestBody {
    @NotBlank
    private String siteUrl;
    private String abonementId = null;
}
