package com.healthcare.appointment.controller;

import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.entity.User;
import com.healthcare.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Book an appointment")
    public ResponseEntity<AppointmentResponse> book(
            @Valid @RequestBody AppointmentRequest req,
            @AuthenticationPrincipal User patient) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(req, patient));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User patient) {
        return ResponseEntity.ok(appointmentService.cancel(id, patient));
    }

    @GetMapping
    @Operation(summary = "Get all my appointments")
    public ResponseEntity<List<AppointmentResponse>> myAppointments(
            @AuthenticationPrincipal User patient) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(patient));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID with event log")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User patient) {
        return ResponseEntity.ok(appointmentService.getById(id, patient));
    }
}
