package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.DoctorResponse;
import com.healthcare.appointment.dto.SlotResponse;
import com.healthcare.appointment.entity.Doctor;
import com.healthcare.appointment.repository.DoctorRepository;
import com.healthcare.appointment.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Doctors & Slots")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final SlotService slotService;

    @GetMapping("/doctors")
    @Operation(summary = "List all doctors")
    public List<DoctorResponse> listDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/slots")
    @Operation(summary = "Get available slots for a doctor on a given date")
    public List<SlotResponse> getSlots(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.getAvailableSlots(doctorId, date);
    }

    private DoctorResponse toDto(Doctor d) {
        return new DoctorResponse(
                d.getId(), d.getName(), d.getHospitalName(),
                d.getSpecialization(),
                d.getAvailableFrom().toString(),
                d.getAvailableTo().toString(),
                d.getSlotDurationMinutes()
        );
    }
}
