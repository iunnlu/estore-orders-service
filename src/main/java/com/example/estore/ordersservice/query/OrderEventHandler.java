package com.example.estore.ordersservice.query;

import com.example.estore.ordersservice.core.data.OrderEntity;
import com.example.estore.ordersservice.core.data.OrderRepository;
import com.example.estore.ordersservice.core.events.OrderApprovedEvent;
import com.example.estore.ordersservice.core.events.OrderCreatedEvent;
import com.example.estore.ordersservice.core.events.OrderRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderEventHandler {
    private final OrderRepository orderRepository;

    @EventHandler
    public void on(OrderCreatedEvent event) throws Exception {
        OrderEntity order = new OrderEntity();
        BeanUtils.copyProperties(event, order);
        orderRepository.save(order);
    }

    @EventHandler
    public void on(OrderApprovedEvent event) throws Exception {
        OrderEntity order = orderRepository.findById(event.getOrderId()).orElse(null);
        if(order != null) {
            order.setOrderStatus(event.getOrderStatus());
            orderRepository.save(order);
        }
    }

    @EventHandler
    public void on(OrderRejectedEvent event) throws Exception {
        OrderEntity order = orderRepository.findById(event.getOrderId()).orElse(null);
        if(order != null) {
            order.setOrderStatus(event.getOrderStatus());
            orderRepository.save(order);
        }
    }
}
