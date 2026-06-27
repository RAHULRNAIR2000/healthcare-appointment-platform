package com.healthcare.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class DoctorResponse {
    private UUID id;
    private String name;
    private String hospitalName;
    private String specialization;
    private String availableFrom;
    private String availableTo;
    private int slotDurationMinutes;
}
