package ru.majordomo.hms.personmgr.common;

import lombok.Getter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public enum DocumentType {
    VIRTUAL_HOSTING_OFERTA("oferta_virtual_hosting"),
    VIRTUAL_HOSTING_CONTRACT,
    VIRTUAL_HOSTING_BUDGET_CONTRACT("hms_virtual_hosting_budget_contract"),
    VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT,
    VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL,
    /** Уведомление о расположении серверов Majordomo на территории РФ */
    VIRTUAL_HOSTING_NOTIFY_RF("hms_notice_rf"),
    REGISTRANT_DOMAIN_CERTIFICATE;

    /** mj-rpc Contract.type */
    @Nullable
    private final String billing2Type;

    public static String getNameForHuman(DocumentType type) {
        switch (type){
            case VIRTUAL_HOSTING_NOTIFY_RF:
                return "Уведомление о расположении серверов Majordomo на территории РФ";

            case VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT:
                return "Дополнительное соглашение";

            case VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL:
                return "Коммерческое предложение";

            case VIRTUAL_HOSTING_OFERTA:
                return "Оферта виртуального хостинга";

            case VIRTUAL_HOSTING_CONTRACT:
                return "Договор виртуального хостинга для юридических лиц";

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                return "Договор виртуального хостинга для государственных и муниципальных учереждений";

            case REGISTRANT_DOMAIN_CERTIFICATE:
                return "Сертификат на домен";

            default:
                return type.name();
        }
    }

    public String getNameForHuman() {
        return getNameForHuman(this);
    }

    DocumentType(String billing2Type) {
        this.billing2Type = billing2Type;
    }

    DocumentType() {
        this.billing2Type = null;
    }
}