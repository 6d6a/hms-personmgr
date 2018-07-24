package ru.majordomo.hms.personmgr.dto.fin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyBill extends BaseModel {
    private String paymentDocuments = "";

    private String clientName = "";

    private String legalAddress = "";

    private String inn = "";

    private String kpp = "";

    private Boolean wasChanged = false;

    @NotNull
    @Indexed
    private LocalDate billDate;

    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Indexed
    private LocalDateTime created;

    @Transient
    private String documentNumber;
}
