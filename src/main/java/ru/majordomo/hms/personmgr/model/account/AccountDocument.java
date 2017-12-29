package ru.majordomo.hms.personmgr.model.account;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Document
public class AccountDocument extends VersionedModelBelongsToPersonalAccount{

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public enum Status{
        NEW,
        PROCESSING,
        CREATED,
        SENT
    }

    private String documentTemplateId;

    private DocumentType documentType;

    private Map<String, String> parameters;

    @CreatedDate
    @Indexed
    private LocalDateTime createdDate;

    @Indexed
    @NotNull
    private Status status = Status.NEW;

    @LastModifiedDate
    private LocalDateTime updateDateTime;

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public LocalDateTime getUpdateDateTime() {
        return updateDateTime;
    }

    public void setUpdateDateTime(LocalDateTime updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getDocumentTemplateId() {
        return documentTemplateId;
    }

    public void setDocumentTemplateId(String documentTemplateId) {
        this.documentTemplateId = documentTemplateId;
    }
}
