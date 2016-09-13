package ru.majordomo.hms.personmgr.common.message.destination;

import ru.majordomo.hms.personmgr.common.message.MessageDestinationType;

/**
 * AmqpMessageDestination
 */
public class AmqpMessageDestination extends GenericMessageDestination {
    private String exchange;

    private String routingKey;

    {
        setType(MessageDestinationType.AMQP);
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    @Override
    public String toString() {
        return "AmqpMessageDestination{" +
                "exchange='" + exchange + '\'' +
                ", routingKey='" + routingKey + '\'' +
                "} " + super.toString();
    }
}
