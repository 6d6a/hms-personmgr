package ru.majordomo.hms.personmgr.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class HistoryRequest {
    @NotBlank(message = "Не указано сообщение")
    private String historyMessage;
    private String operator;
}
