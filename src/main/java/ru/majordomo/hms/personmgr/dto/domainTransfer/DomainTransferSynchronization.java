package ru.majordomo.hms.personmgr.dto.domainTransfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainTransferSynchronization {
    private final String domainName;
    private final String transferStatus;

    private String validationError;

    public DomainTransferSynchronization(
            @JsonProperty("domainName") String domainName,
            @JsonProperty("transferStatus") String transferStatus
    ) {
        this.domainName = domainName;
        this.transferStatus = transferStatus;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getValidationError() {
        return validationError;
    }

    public boolean isTransferAccepted() {
        return transferStatus.equals(Constants.DOMAIN_TRANSFER_STATUS_ACCEPTED);
    }

    public boolean isTransferRejected() {
        return transferStatus.equals(Constants.DOMAIN_TRANSFER_STATUS_REJECTED);
    }

    public boolean isTransferCancelled() {
        return transferStatus.equals(Constants.DOMAIN_TRANSFER_STATUS_CANCELLED);
    }

    public boolean isValid() {
        if (transferStatus == null || transferStatus.equals("")) {
            validationError = "Отсутствует статус трансфера";
            return false;
        }
        if (!isTransferAccepted() && !isTransferRejected() && !isTransferCancelled()) {
            validationError = "Передан неизвестный статус трансфера";
            return false;
        }
        if (domainName == null || domainName.equals("")) {
            validationError = "Отсутствует доменное имя";
            return false;
        }

        return true;
    }

    private static class Constants {
        public static final String DOMAIN_TRANSFER_STATUS_ACCEPTED = "accepted";
        public static final String DOMAIN_TRANSFER_STATUS_REJECTED = "rejected";
        public static final String DOMAIN_TRANSFER_STATUS_CANCELLED = "cancelled";
    }
}
