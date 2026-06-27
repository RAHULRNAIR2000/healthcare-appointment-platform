package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.SlotResponse;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.Doctor;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public List<SlotResponse> getAvailableSlots(UUID doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        ZoneOffset zone = ZoneOffset.UTC;
        OffsetDateTime dayStart = date.atTime(doctor.getAvailableFrom()).atOffset(zone);
        OffsetDateTime dayEnd = date.atTime(doctor.getAvailableTo()).atOffset(zone);

        List<Appointment> booked = appointmentRepository.findActiveByDoctorAndDate(
                doctorId,
                date.atStartOfDay().atOffset(zone),
                date.plusDays(1).atStartOfDay().atOffset(zone)
        );

        Set<OffsetDateTime> bookedStarts = booked.stream()
                .map(Appointment::getSlotStart)
                .collect(Collectors.toSet());

        List<SlotResponse> slots = new ArrayList<>();
        OffsetDateTime current = dayStart;
        while (current.plusMinutes(doctor.getSlotDurationMinutes()).compareTo(dayEnd) <= 0) {
            if (!bookedStarts.contains(current)) {
                slots.add(new SlotResponse(current, current.plusMinutes(doctor.getSlotDurationMinutes())));
            }
            current = current.plusMinutes(doctor.getSlotDurationMinutes());
        }
        return slots;
    }
}
