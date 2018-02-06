package ru.majordomo.hms.personmgr.model.order;

import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Document
public class AccountOrder extends ModelBelongsToPersonalAccount {

    @NotNull
    private OrderState state;

    @NotNull
    private String operator;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;


    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "AccountOrder{" +
                "state='" + state + '\'' +
                ", operator=" + operator +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }
}
