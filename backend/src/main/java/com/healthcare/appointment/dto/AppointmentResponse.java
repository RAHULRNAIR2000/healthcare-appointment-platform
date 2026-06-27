package com.healthcare.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class AppointmentResponse {
    private UUID id;
    private String doctorName;
    private String hospitalName;
    private String specialization;
    private OffsetDateTime slotStart;
    private OffsetDateTime slotEnd;
    private String status;
    private String notes;
    private OffsetDateTime createdAt;
    private List<AppointmentLogResponse> logs;
}
