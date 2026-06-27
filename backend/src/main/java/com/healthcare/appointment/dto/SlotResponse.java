package com.healthcare.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class SlotResponse {
    private OffsetDateTime slotStart;
    private OffsetDateTime slotEnd;
}
