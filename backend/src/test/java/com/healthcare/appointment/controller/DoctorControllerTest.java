package com.healthcare.appointment.controller;

import com.healthcare.appointment.config.SecurityConfig;
import com.healthcare.appointment.dto.SlotResponse;
import com.healthcare.appointment.entity.Doctor;
import com.healthcare.appointment.entity.User;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.repository.DoctorRepository;
import com.healthcare.appointment.repository.UserRepository;
import com.healthcare.appointment.security.JwtFilter;
import com.healthcare.appointment.security.JwtUtil;
import com.healthcare.appointment.service.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoctorController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class DoctorControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean DoctorRepository doctorRepository;
    @MockBean SlotService slotService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserRepository userRepository;

    private UUID doctorId;
    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        validToken = "valid.jwt.token";

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hash")
                .build();

        when(jwtUtil.isValid(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn(testUser.getId().toString());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    }

    // --- GET /api/doctors ---

    @Test
    void listDoctors_withValidToken_returns200AndDoctorList() throws Exception {
        Doctor doctor = Doctor.builder()
                .id(doctorId)
                .name("Dr. Anjali Sharma")
                .hospitalName("Apollo Hospital")
                .specialization("Cardiology")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .build();
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));

        mockMvc.perform(get("/api/doctors")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Anjali Sharma"))
                .andExpect(jsonPath("$[0].hospitalName").value("Apollo Hospital"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));
    }

    @Test
    void listDoctors_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/slots ---

    @Test
    void getSlots_validDoctorAndDate_returns200AndSlotList() throws Exception {
        OffsetDateTime slot1Start = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime slot1End = slot1Start.plusMinutes(30);
        when(slotService.getAvailableSlots(eq(doctorId), any()))
                .thenReturn(List.of(new SlotResponse(slot1Start, slot1End)));

        mockMvc.perform(get("/api/slots")
                        .param("doctorId", doctorId.toString())
                        .param("date", "2026-07-01")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotStart").exists())
                .andExpect(jsonPath("$[0].slotEnd").exists());
    }

    @Test
    void getSlots_invalidDoctorId_returns404() throws Exception {
        when(slotService.getAvailableSlots(eq(doctorId), any()))
                .thenThrow(new ResourceNotFoundException("Doctor not found"));

        mockMvc.perform(get("/api/slots")
                        .param("doctorId", doctorId.toString())
                        .param("date", "2026-07-01")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Doctor not found"));
    }

    @Test
    void getSlots_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/slots")
                        .param("doctorId", doctorId.toString())
                        .param("date", "2026-07-01"))
                .andExpect(status().isForbidden());
    }
}
