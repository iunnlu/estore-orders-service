package com.example.estore.ordersservice.core.events;

import com.example.estore.ordersservice.core.enums.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {
    private final String orderId;
    private final OrderStatus orderStatus = OrderStatus.APPROVED;
}
