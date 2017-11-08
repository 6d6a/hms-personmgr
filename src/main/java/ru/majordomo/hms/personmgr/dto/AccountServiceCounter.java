package ru.majordomo.hms.personmgr.dto;

public class AccountServiceCounter extends ResourceCounter{
    private int quantity;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
