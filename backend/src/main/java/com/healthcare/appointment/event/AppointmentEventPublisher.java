package com.healthcare.appointment.event;

import com.healthcare.appointment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBookedEvent(AppointmentBookedEvent event) {
        log.info("Publishing appointment.booked event for appointmentId={}", event.getAppointmentId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }
}
