package com.example.estore.ordersservice.command;

import com.example.estore.ordersservice.core.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateOrderCommand {
    public final String orderId;
    private final String userId;
    private final String productId;
    private final int quantity;
    private final String addressId;
    private final OrderStatus orderStatus;
}
