package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.SlotResponse;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.Doctor;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock DoctorRepository doctorRepository;
    @Mock AppointmentRepository appointmentRepository;

    @InjectMocks SlotService slotService;

    private UUID doctorId;
    private Doctor doctor;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        date = LocalDate.of(2026, 7, 1);

        doctor = Doctor.builder()
                .id(doctorId)
                .name("Dr. Anjali Sharma")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .build();
    }

    @Test
    void getAvailableSlots_doctorNotFound_throwsResourceNotFoundException() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.getAvailableSlots(doctorId, date))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Doctor not found");
    }

    @Test
    void getAvailableSlots_noBookings_returnsAllSlots() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndDate(eq(doctorId), any(), any()))
                .thenReturn(List.of());

        List<SlotResponse> slots = slotService.getAvailableSlots(doctorId, date);

        // 09:00 to 17:00, 30-min slots = 16 slots
        assertThat(slots).hasSize(16);
        assertThat(slots.get(0).getSlotStart())
                .isEqualTo(OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC));
        assertThat(slots.get(slots.size() - 1).getSlotEnd())
                .isEqualTo(OffsetDateTime.of(2026, 7, 1, 17, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void getAvailableSlots_oneSlotBooked_excludesThatSlot() {
        OffsetDateTime bookedSlot = OffsetDateTime.of(2026, 7, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        Appointment booked = Appointment.builder().slotStart(bookedSlot).build();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndDate(eq(doctorId), any(), any()))
                .thenReturn(List.of(booked));

        List<SlotResponse> slots = slotService.getAvailableSlots(doctorId, date);

        assertThat(slots).hasSize(15);
        assertThat(slots).noneMatch(s -> s.getSlotStart().equals(bookedSlot));
    }

    @Test
    void getAvailableSlots_allSlotsBooked_returnsEmptyList() {
        ZoneOffset utc = ZoneOffset.UTC;
        List<Appointment> allBooked = List.of(
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,9,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,9,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,10,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,10,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,11,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,11,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,12,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,12,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,13,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,13,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,14,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,14,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,15,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,15,30,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,16,0,0,0,utc)).build(),
                Appointment.builder().slotStart(OffsetDateTime.of(2026,7,1,16,30,0,0,utc)).build()
        );

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndDate(eq(doctorId), any(), any()))
                .thenReturn(allBooked);

        List<SlotResponse> slots = slotService.getAvailableSlots(doctorId, date);

        assertThat(slots).isEmpty();
    }

    @Test
    void getAvailableSlots_anjaliSharma9to17_returns16Slots() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndDate(eq(doctorId), any(), any()))
                .thenReturn(List.of());

        List<SlotResponse> slots = slotService.getAvailableSlots(doctorId, date);

        assertThat(slots).hasSize(16);
        // Each slot is exactly 30 minutes
        slots.forEach(s ->
                assertThat(s.getSlotEnd()).isEqualTo(s.getSlotStart().plusMinutes(30))
        );
    }
}
