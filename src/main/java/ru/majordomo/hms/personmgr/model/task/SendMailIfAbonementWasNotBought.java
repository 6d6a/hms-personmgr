package ru.majordomo.hms.personmgr.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SendMailIfAbonementWasNotBought extends AccountTask {

    private Boolean emailWasSent;

    public SendMailIfAbonementWasNotBought(String personalAccountId, LocalDateTime execAfter) {
        setPersonalAccountId(personalAccountId);
        setExecAfter(execAfter);
    }
}
