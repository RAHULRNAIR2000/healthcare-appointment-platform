package com.healthcare.appointment.service;

import com.healthcare.appointment.dto.AppointmentLogResponse;
import com.healthcare.appointment.dto.AppointmentRequest;
import com.healthcare.appointment.dto.AppointmentResponse;
import com.healthcare.appointment.entity.Appointment;
import com.healthcare.appointment.entity.AppointmentLog;
import com.healthcare.appointment.entity.Doctor;
import com.healthcare.appointment.entity.User;
import com.healthcare.appointment.event.AppointmentBookedEvent;
import com.healthcare.appointment.event.AppointmentEventPublisher;
import com.healthcare.appointment.exception.ResourceNotFoundException;
import com.healthcare.appointment.repository.AppointmentLogRepository;
import com.healthcare.appointment.repository.AppointmentRepository;
import com.healthcare.appointment.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLogRepository appointmentLogRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentEventPublisher eventPublisher;

    @Transactional
    public AppointmentResponse book(AppointmentRequest req, User patient) {
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        boolean slotTaken = appointmentRepository
                .findActiveByDoctorAndSlot(doctor.getId(), req.getSlotStart())
                .isPresent();

        if (slotTaken) {
            throw new IllegalStateException("This slot is already booked");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .slotStart(req.getSlotStart())
                .slotEnd(req.getSlotStart().plusMinutes(doctor.getSlotDurationMinutes()))
                .status("PENDING")
                .notes(req.getNotes())
                .build();
        appointmentRepository.save(appointment);

        AppointmentLog log = AppointmentLog.builder()
                .appointment(appointment)
                .eventType("BOOKED")
                .message("Appointment booked by " + patient.getName())
                .build();
        appointmentLogRepository.save(log);

        eventPublisher.publishBookedEvent(new AppointmentBookedEvent(
                appointment.getId(),
                patient.getEmail(),
                patient.getName(),
                doctor.getName(),
                doctor.getHospitalName(),
                appointment.getSlotStart(),
                appointment.getSlotEnd()
        ));

        return toResponse(appointment, List.of(log));
    }

    @Transactional
    public AppointmentResponse cancel(UUID appointmentId, User patient) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }
        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new IllegalStateException("Appointment is already cancelled");
        }

        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);

        AppointmentLog log = AppointmentLog.builder()
                .appointment(appointment)
                .eventType("CANCELLED")
                .message("Appointment cancelled by " + patient.getName())
                .build();
        appointmentLogRepository.save(log);

        List<AppointmentLog> logs = appointmentLogRepository
                .findByAppointmentIdOrderByCreatedAtAsc(appointmentId);
        return toResponse(appointment, logs);
    }

    public List<AppointmentResponse> getMyAppointments(User patient) {
        return appointmentRepository.findByPatientIdOrderBySlotStartDesc(patient.getId())
                .stream()
                .map(a -> toResponse(a, List.of()))
                .collect(Collectors.toList());
    }

    public AppointmentResponse getById(UUID appointmentId, User patient) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        List<AppointmentLog> logs = appointmentLogRepository
                .findByAppointmentIdOrderByCreatedAtAsc(appointmentId);
        return toResponse(appointment, logs);
    }

    private AppointmentResponse toResponse(Appointment a, List<AppointmentLog> logs) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .doctorName(a.getDoctor().getName())
                .hospitalName(a.getDoctor().getHospitalName())
                .specialization(a.getDoctor().getSpecialization())
                .slotStart(a.getSlotStart())
                .slotEnd(a.getSlotEnd())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .logs(logs.stream().map(l -> new AppointmentLogResponse(
                        l.getEventType(), l.getMessage(), l.getCreatedAt()
                )).collect(Collectors.toList()))
                .build();
    }
}
