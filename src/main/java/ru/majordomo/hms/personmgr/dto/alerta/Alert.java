package ru.majordomo.hms.personmgr.dto.alerta;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import ru.majordomo.hms.personmgr.dto.alerta.Severity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class Alert {
    /** event name */
    @Nonnull
    @NotBlank
    private AlertaEvent event;

    /** resource under alarm */
    @Nonnull
    @NotBlank
    private String resource;

    private Severity severity;

    /** freeform text description */
    private String text;

    /** event value */
    private String value;

    /** list of effected services */
    private List<String> service = new ArrayList<>();

    @Nullable
    private AlertStatus status;

}
