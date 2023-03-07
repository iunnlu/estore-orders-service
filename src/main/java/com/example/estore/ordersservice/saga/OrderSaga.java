package com.example.estore.ordersservice.saga;

import com.example.estore.core.commands.CancelProductReservationCommand;
import com.example.estore.core.commands.ProcessPaymentCommand;
import com.example.estore.core.commands.ReserveProductCommand;
import com.example.estore.core.events.PaymentProcessedEvent;
import com.example.estore.core.events.ProductReservationCancelEvent;
import com.example.estore.core.events.ProductReservedEvent;
import com.example.estore.core.model.User;
import com.example.estore.core.query.FetchUserPaymentDetailsQuery;
import com.example.estore.ordersservice.command.ApproveOrderCommand;
import com.example.estore.ordersservice.command.RejectOrderCommand;
import com.example.estore.ordersservice.core.events.OrderApprovedEvent;
import com.example.estore.ordersservice.core.events.OrderCreatedEvent;
import com.example.estore.ordersservice.core.events.OrderRejectedEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Saga
public class OrderSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient DeadlineManager deadlineManager;
    @Autowired
    private transient QueryGateway queryGateway;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

    private String scheduleId;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {
        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .productId(orderCreatedEvent.getProductId())
                .quantity(orderCreatedEvent.getQuantity())
                .userId(orderCreatedEvent.getUserId())
                .build();

        LOGGER.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + " and productId: " + reserveProductCommand.getProductId());

        commandGateway.send(reserveProductCommand, (commandMessage, commandResultMessage) -> {
            if(commandResultMessage.isExceptional()) {
                //Start a compensating transaction
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservedEvent productReservedEvent) {
        //Process user payment
        LOGGER.info("ProductReservedEvent is called for productId: " + productReservedEvent.getProductId() + " and orderId: " + productReservedEvent.getOrderId());
        FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery = new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());
        User user = null;
        try {
            user = queryGateway.query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class)).join();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());

            cancelProductReservation(productReservedEvent, e.getMessage());
            return;
        }
        if(user == null) {
            cancelProductReservation(productReservedEvent, "Could not fetch user payment details.");
            return;
        }
        LOGGER.info("Successfully fetched user payment details for user " + user.getFirstName());

        scheduleId = deadlineManager.schedule(Duration.of(10, ChronoUnit.SECONDS), "payment-processing-deadline", productReservedEvent);

        if(true)
            return;

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentDetails(user.getPaymentDetails())
                .paymentId(UUID.randomUUID().toString())
                .build();

        String result = null;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            cancelProductReservation(productReservedEvent, ex.getMessage());
            return;
        }

        if(result == null) {
            //Start comp transaction
            LOGGER.info("Start compensating transaction");
            cancelProductReservation(productReservedEvent, "Could not process user payment with provided payment details");
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservationCancelEvent productReservationCancelEvent) {
        RejectOrderCommand rejectOrderCommand =
                new RejectOrderCommand(productReservationCancelEvent.getOrderId(), productReservationCancelEvent.getReason());

        commandGateway.send(rejectOrderCommand);
    }

    private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {
        cancelDeadline();

        CancelProductReservationCommand cancelProductReservationCommand =
                CancelProductReservationCommand.builder()
                        .orderId(productReservedEvent.getOrderId())
                        .productId(productReservedEvent.getProductId())
                        .quantity(productReservedEvent.getQuantity())
                        .userId(productReservedEvent.getUserId())
                        .reason(reason)
                        .build();

        commandGateway.send(cancelProductReservationCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentProcessedEvent paymentProcessedEvent) {
        cancelDeadline();

        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessedEvent.getOrderId());
        commandGateway.send(approveOrderCommand);
    }

    private void cancelDeadline() {
        if(scheduleId != null) {
            deadlineManager.cancelSchedule("payment-processing-deadline", scheduleId);
        }
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        LOGGER.info("Order is approved. Completed for orderId: " + orderApprovedEvent.getOrderId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent) {
        LOGGER.info("Order is rejected. Completed for orderId: " + orderRejectedEvent.getOrderId() + ", reason: " + orderRejectedEvent.getReason());
    }

    @DeadlineHandler(deadlineName = "payment-processing-deadline")
    public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {
        LOGGER.info("Payment processing deadline took place.");
        cancelProductReservation(productReservedEvent, "Payment timeout.");
    }
}
