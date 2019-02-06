package ru.majordomo.hms.personmgr.dto.revisium;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class RevisiumRequestBody {
    @NotBlank
    private String siteUrl;
    private String abonementId = null;
}
