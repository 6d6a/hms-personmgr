package ru.majordomo.hms.personmgr.model.order.documentOrder;

public enum DeliveryType {
    FREE_DELIVERY,
    FAST_PAID_DELIVERY,
    REGULAR_PAID_DELIVERY;

    public String humanize() {
        switch (this) {
            case FAST_PAID_DELIVERY:
                return "ускоренная платная доставка";
            case REGULAR_PAID_DELIVERY:
                return "платная доставка";
            case FREE_DELIVERY:
                return "бесплатная доставка";
            default:
                return this.name();
        }
    }
}
