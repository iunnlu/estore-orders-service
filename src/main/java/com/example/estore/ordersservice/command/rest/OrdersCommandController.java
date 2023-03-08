package com.example.estore.ordersservice.command.rest;

import com.example.estore.ordersservice.command.CreateOrderCommand;
import com.example.estore.ordersservice.core.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/orders")
public class OrdersCommandController {
    private final CommandGateway commandGateway;

    @GetMapping
    public String test() {
        return "TEST";
    }

    @PostMapping()
    public String createOrder(@RequestBody CreateOrderRestModel createOrderRestModel) {
        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .orderId(UUID.randomUUID().toString())
                .userId("27b95829-4f3f-4ddf-8983-151ba010e35b")
                .productId(createOrderRestModel.getProductId())
                .quantity(createOrderRestModel.getQuantity())
                .addressId(createOrderRestModel.getAddressId())
                .orderStatus(OrderStatus.CREATED)
                .build();

        String returnValue = commandGateway.sendAndWait(createOrderCommand);

        return returnValue;
    }
}
