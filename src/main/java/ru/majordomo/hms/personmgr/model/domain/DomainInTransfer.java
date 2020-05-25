package ru.majordomo.hms.personmgr.model.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Document
@EqualsAndHashCode(callSuper = true)
public class DomainInTransfer extends ModelBelongsToPersonalAccount {
    @Indexed
    @NotBlank
    private String personId;

    @Indexed
    @NotBlank
    private String domainName;

    private String documentNumber;

    @Indexed
    private State state;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updated;

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName.toLowerCase();
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public enum State {
        PROCESSING,
        ACCEPTED,
        REJECTED,
        NEED_TO_PROCESS,
        CANCELLED,
    }

    @Override
    public String toString() {
        return "DomainInTransfer{" +
                "personId=" + personId +
                "domainName=" + domainName +
                "documentNumber=" + documentNumber +
                "state=" + state +
                "created=" + created +
                "updated=" + updated +
                "} " + super.toString();
    }
}
