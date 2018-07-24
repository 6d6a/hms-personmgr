package ru.majordomo.hms.personmgr.model.order.documentOrder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
public class ActOfReconciliation implements Doc {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "должна быть задана")
    LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "должна быть задана")
    LocalDate endDate;

    @Override
    public String humanize() {
        return "Акт сверки от " + this.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + " до " + this.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ";
    }
}
