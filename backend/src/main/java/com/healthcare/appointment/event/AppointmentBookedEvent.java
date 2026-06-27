package com.healthcare.appointment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentBookedEvent {
    private UUID appointmentId;
    private String patientEmail;
    private String patientName;
    private String doctorName;
    private String hospitalName;
    private OffsetDateTime slotStart;
    private OffsetDateTime slotEnd;
}
