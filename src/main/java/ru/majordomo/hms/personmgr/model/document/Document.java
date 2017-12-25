package ru.majordomo.hms.personmgr.model.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.common.FileType;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public abstract class Document extends VersionedModelBelongsToPersonalAccount{

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public enum Status{
        ORDERED,
        PROCESSING,
        CREATED,
        SENT
    }
    private DocumentType documentType;

    private Map<String, Object> parameters;

    @CreatedDate
    @Indexed
    @NotNull
    private LocalDateTime orderDate;

    @NotNull
    private Status status;

    @LastModifiedDate
    private LocalDateTime updateDateTime;

    public DocumentType getDocumentType() {
        return documentType;
    }

    private FileType fileType;

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public LocalDateTime getUpdateDateTime() {
        return updateDateTime;
    }

    public void setUpdateDateTime(LocalDateTime updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
