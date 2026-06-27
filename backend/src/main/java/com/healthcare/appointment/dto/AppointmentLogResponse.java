package com.healthcare.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AppointmentLogResponse {
    private String eventType;
    private String message;
    private OffsetDateTime createdAt;
}
