package ru.majordomo.hms.personmgr.dto.fin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingOperation extends BaseModel {
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Indexed
    private LocalDateTime created;

    @NotNull
    @Indexed
    private LocalDate billDate;

    private String type;

    private BigDecimal amount;
    private BigDecimal available;
    /**
     * Номер платежного документа у платежа и списания в finansier.BillingOperation.documentNumber
     * Уникальный для платежа.
     * У 2х может быть один и тот же номер документа если большое списание было разбито на несколько платежей
     */
    private String documentNumber;
    private String duplicateDocumentNumber;
    private String comment;
    private String operator;

    private Map<String, Object> paymentData;
    private Map<String, Object> withdrawalData;
}
