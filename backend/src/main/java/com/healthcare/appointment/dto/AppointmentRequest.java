package com.healthcare.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AppointmentRequest {
    @NotNull
    private UUID doctorId;

    @NotNull
    private OffsetDateTime slotStart;

    private String notes;
}
