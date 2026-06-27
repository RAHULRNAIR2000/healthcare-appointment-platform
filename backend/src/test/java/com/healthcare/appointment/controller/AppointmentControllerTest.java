package com.healthcare.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.appointment.config.SecurityConfig;
import com.healthcare.appointment.dto.AppointmentLogResponse;
import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.entity.User;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.repository.UserRepository;
import com.healthcare.appointment.security.JwtFilter;
import com.healthcare.appointment.security.JwtUtil;
import com.healthcare.appointment.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class AppointmentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AppointmentService appointmentService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserRepository userRepository;

    private UUID appointmentId;
    private UUID doctorId;
    private User testUser;
    private String validToken;
    private OffsetDateTime slotStart;
    private AppointmentResponse mockAppointmentResponse;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        validToken = "valid.jwt.token";
        slotStart = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("patient@example.com")
                .name("John Doe")
                .passwordHash("hash")
                .build();

        mockAppointmentResponse = AppointmentResponse.builder()
                .id(appointmentId)
                .doctorName("Dr. Anjali Sharma")
                .hospitalName("Apollo Hospital")
                .specialization("Cardiology")
                .slotStart(slotStart)
                .slotEnd(slotStart.plusMinutes(30))
                .status("PENDING")
                .logs(List.of())
                .build();

        when(jwtUtil.isValid(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn(testUser.getId().toString());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    }

    // --- POST /api/appointments ---

    @Test
    void book_validRequest_returns201WithPendingStatus() throws Exception {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctorId);
        req.setSlotStart(slotStart);

        when(appointmentService.book(any(AppointmentRequest.class), any(User.class)))
                .thenReturn(mockAppointmentResponse);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Anjali Sharma"));
    }

    @Test
    void book_slotAlreadyTaken_returns409() throws Exception {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctorId);
        req.setSlotStart(slotStart);

        when(appointmentService.book(any(AppointmentRequest.class), any(User.class)))
                .thenThrow(new IllegalStateException("This slot is already booked"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This slot is already booked"));
    }

    @Test
    void book_missingDoctorId_returns400() throws Exception {
        AppointmentRequest req = new AppointmentRequest();
        req.setSlotStart(slotStart);
        // doctorId intentionally null

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.doctorId").exists());
    }

    @Test
    void book_withoutToken_returns403() throws Exception {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctorId);
        req.setSlotStart(slotStart);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/appointments/{id} ---

    @Test
    void cancel_ownAppointment_returns200WithCancelledStatus() throws Exception {
        AppointmentResponse cancelled = AppointmentResponse.builder()
                .id(appointmentId).doctorName("Dr. Anjali Sharma")
                .hospitalName("Apollo Hospital").specialization("Cardiology")
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("CANCELLED").logs(List.of()).build();

        when(appointmentService.cancel(eq(appointmentId), any(User.class))).thenReturn(cancelled);

        mockMvc.perform(delete("/api/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_anotherPatientsAppointment_returns400() throws Exception {
        when(appointmentService.cancel(eq(appointmentId), any(User.class)))
                .thenThrow(new IllegalArgumentException("You can only cancel your own appointments"));

        mockMvc.perform(delete("/api/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You can only cancel your own appointments"));
    }

    @Test
    void cancel_alreadyCancelled_returns409() throws Exception {
        when(appointmentService.cancel(eq(appointmentId), any(User.class)))
                .thenThrow(new IllegalStateException("Appointment is already cancelled"));

        mockMvc.perform(delete("/api/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Appointment is already cancelled"));
    }

    @Test
    void cancel_appointmentNotFound_returns404() throws Exception {
        when(appointmentService.cancel(eq(appointmentId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Appointment not found"));

        mockMvc.perform(delete("/api/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Appointment not found"));
    }

    // --- GET /api/appointments ---

    @Test
    void myAppointments_withValidToken_returns200AndList() throws Exception {
        when(appointmentService.getMyAppointments(any(User.class)))
                .thenReturn(List.of(mockAppointmentResponse));

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].doctorName").value("Dr. Anjali Sharma"));
    }

    // --- GET /api/appointments/{id} ---

    @Test
    void getById_ownAppointment_returns200WithLogs() throws Exception {
        AppointmentResponse withLogs = AppointmentResponse.builder()
                .id(appointmentId).doctorName("Dr. Anjali Sharma")
                .hospitalName("Apollo Hospital").specialization("Cardiology")
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("CONFIRMED")
                .logs(List.of(
                        new AppointmentLogResponse("BOOKED", "Booked by John Doe", OffsetDateTime.now()),
                        new AppointmentLogResponse("CONFIRMED", "Confirmed by worker", OffsetDateTime.now())
                ))
                .build();

        when(appointmentService.getById(eq(appointmentId), any(User.class))).thenReturn(withLogs);

        mockMvc.perform(get("/api/appointments/" + appointmentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.logs.length()").value(2))
                .andExpect(jsonPath("$.logs[0].eventType").value("BOOKED"));
    }
}
