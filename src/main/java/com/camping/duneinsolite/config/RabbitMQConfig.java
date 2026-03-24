package com.camping.duneinsolite.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── 1. Constants — names of everything ───────────────────────
    public static final String NOTIFICATION_QUEUE    = "notification.queue";
    public static final String NOTIFICATION_DLQ      = "notification.dlq";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DLQ_EXCHANGE          = "notification.dlq.exchange";

    // ── 2. Routing keys — what each event is called ───────────────
    public static final String RESERVATION_CREATED   = "reservation.created";
    public static final String RESERVATION_CONFIRMED = "reservation.confirmed";
    public static final String RESERVATION_REJECTED  = "reservation.rejected";
    public static final String RESERVATION_UPDATED   = "reservation.updated";

    // ── 3. Dead Letter Queue setup ────────────────────────────────

    // 3a. The DLQ itself — durable means it survives RabbitMQ restart
    @Bean
    public Queue notificationDLQ() {
        return QueueBuilder
                .durable(NOTIFICATION_DLQ)
                .build();
    }

    // 3b. DLQ exchange — a simple direct exchange just for the DLQ
    //     when a message fails it lands here
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    // 3c. Bind DLQ to its exchange
    //     routingKey = NOTIFICATION_DLQ
    //     failed messages arrive with this key automatically
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(notificationDLQ())
                .to(dlqExchange())
                .with(NOTIFICATION_DLQ);
    }

    // ── 4. Main notification queue ────────────────────────────────
    // durable = survives RabbitMQ restart ✅
    // x-dead-letter-exchange = if message fails → send to DLQ exchange
    // x-dead-letter-routing-key = with this key so DLQ exchange
    //                             routes it to notification.dlq
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    // ── 5. Topic Exchange ─────────────────────────────────────────
    // this is our main exchange
    // all producers publish here
    // it routes to queues based on routing key pattern
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    // ── 6. Binding — connect queue to exchange ────────────────────
    // pattern "#" means match EVERYTHING
    // reservation.created   ✅ matched
    // reservation.confirmed ✅ matched
    // payment.received      ✅ matched (future)
    // anything.anything     ✅ matched
    // when you add new events later → zero changes needed here
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with("#");
    }

    // ── 7. JSON Message Converter ─────────────────────────────────
    // by default RabbitMQ sends messages as raw bytes
    // this converter automatically:
    //   serializes   Java object → JSON when publishing
    //   deserializes JSON → Java object when consuming
    // so you work with NotificationMessage objects not raw bytes
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ── 8. RabbitTemplate ─────────────────────────────────────────
    // this is the object you inject in NotificationPublisher
    // to send messages
    // we attach our JSON converter so it serializes automatically
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
