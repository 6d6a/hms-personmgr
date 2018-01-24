package ru.majordomo.hms.personmgr.common;

public enum DocumentType {
    VIRTUAL_HOSTING_OFERTA,
    VIRTUAL_HOSTING_CONTRACT,
    VIRTUAL_HOSTING_BUDGET_CONTRACT,
    VIRTUAL_HOSTING_BUDGET_SUPPLEMENTARY_AGREEMENT,
    VIRTUAL_HOSTING_COMMERCIAL_PROPOSAL,
    VIRTUAL_HOSTING_NOTIFY_RF,
    REGISTRANT_DOMAIN_CERTIFICATE;

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
}