package com.healthcare.appointment.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentLogRepository appointmentLogRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock AppointmentEventPublisher eventPublisher;

    @InjectMocks AppointmentService appointmentService;

    private UUID doctorId;
    private UUID patientId;
    private UUID appointmentId;
    private Doctor doctor;
    private User patient;
    private AppointmentRequest request;
    private OffsetDateTime slotStart;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        slotStart = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        doctor = Doctor.builder()
                .id(doctorId)
                .name("Dr. Anjali Sharma")
                .hospitalName("Apollo Hospital")
                .specialization("Cardiology")
                .availableFrom(LocalTime.of(9, 0))
                .availableTo(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .build();

        patient = User.builder()
                .id(patientId)
                .email("patient@example.com")
                .name("John Doe")
                .passwordHash("hash")
                .build();

        request = new AppointmentRequest();
        request.setDoctorId(doctorId);
        request.setSlotStart(slotStart);
        request.setNotes("Regular checkup");
    }

    // --- book() ---

    @Test
    void book_success_savesPendingAppointmentAndPublishesEvent() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndSlot(doctorId, slotStart)).thenReturn(Optional.empty());

        Appointment saved = Appointment.builder()
                .id(appointmentId).patient(patient).doctor(doctor)
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("PENDING").notes("Regular checkup").build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentLog log = AppointmentLog.builder()
                .appointment(saved).eventType("BOOKED")
                .message("Appointment booked by John Doe").build();
        when(appointmentLogRepository.save(any(AppointmentLog.class))).thenReturn(log);

        AppointmentResponse response = appointmentService.book(request, patient);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getDoctorName()).isEqualTo("Dr. Anjali Sharma");
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentLogRepository).save(any(AppointmentLog.class));
        verify(eventPublisher).publishBookedEvent(any(AppointmentBookedEvent.class));
    }

    @Test
    void book_publishesEventWithCorrectPayload() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndSlot(doctorId, slotStart)).thenReturn(Optional.empty());

        Appointment saved = Appointment.builder()
                .id(appointmentId).patient(patient).doctor(doctor)
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("PENDING").build();
        when(appointmentRepository.save(any())).thenReturn(saved);
        when(appointmentLogRepository.save(any())).thenReturn(AppointmentLog.builder()
                .appointment(saved).eventType("BOOKED").build());

        appointmentService.book(request, patient);

        ArgumentCaptor<AppointmentBookedEvent> captor = ArgumentCaptor.forClass(AppointmentBookedEvent.class);
        verify(eventPublisher).publishBookedEvent(captor.capture());

        AppointmentBookedEvent event = captor.getValue();
        assertThat(event.getPatientEmail()).isEqualTo("patient@example.com");
        assertThat(event.getDoctorName()).isEqualTo("Dr. Anjali Sharma");
        assertThat(event.getHospitalName()).isEqualTo("Apollo Hospital");
        assertThat(event.getSlotStart()).isEqualTo(slotStart);
    }

    @Test
    void book_doctorNotFound_throwsResourceNotFoundException() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.book(request, patient))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Doctor not found");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void book_slotAlreadyTaken_throwsIllegalStateException() {
        Appointment existing = Appointment.builder().id(UUID.randomUUID()).build();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findActiveByDoctorAndSlot(doctorId, slotStart))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appointmentService.book(request, patient))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This slot is already booked");

        verify(appointmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishBookedEvent(any());
    }

    // --- cancel() ---

    @Test
    void cancel_success_setsStatusCancelledAndSavesLog() {
        Appointment appointment = Appointment.builder()
                .id(appointmentId).patient(patient).doctor(doctor)
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("PENDING").build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentLogRepository.save(any())).thenReturn(
                AppointmentLog.builder().appointment(appointment).eventType("CANCELLED").build());
        when(appointmentLogRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId))
                .thenReturn(List.of());

        AppointmentResponse response = appointmentService.cancel(appointmentId, patient);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(appointmentLogRepository).save(argThat(l -> "CANCELLED".equals(l.getEventType())));
    }

    @Test
    void cancel_notOwnAppointment_throwsIllegalArgumentException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Appointment appointment = Appointment.builder()
                .id(appointmentId).patient(otherUser).doctor(doctor)
                .status("PENDING").build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(appointmentId, patient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You can only cancel your own appointments");
    }

    @Test
    void cancel_alreadyCancelled_throwsIllegalStateException() {
        Appointment appointment = Appointment.builder()
                .id(appointmentId).patient(patient).doctor(doctor)
                .status("CANCELLED").build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(appointmentId, patient))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Appointment is already cancelled");
    }

    @Test
    void cancel_appointmentNotFound_throwsResourceNotFoundException() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancel(appointmentId, patient))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Appointment not found");
    }

    // --- getMyAppointments() ---

    @Test
    void getMyAppointments_returnsOnlyPatientsAppointments() {
        Appointment a1 = Appointment.builder().id(UUID.randomUUID()).patient(patient).doctor(doctor)
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30)).status("PENDING").build();
        Appointment a2 = Appointment.builder().id(UUID.randomUUID()).patient(patient).doctor(doctor)
                .slotStart(slotStart.plusHours(1)).slotEnd(slotStart.plusHours(1).plusMinutes(30))
                .status("CONFIRMED").build();
        when(appointmentRepository.findByPatientIdOrderBySlotStartDesc(patientId))
                .thenReturn(List.of(a1, a2));

        List<AppointmentResponse> result = appointmentService.getMyAppointments(patient);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AppointmentResponse::getStatus)
                .containsExactly("PENDING", "CONFIRMED");
    }

    // --- getById() ---

    @Test
    void getById_success_returnsAppointmentWithLogs() {
        AppointmentLog log1 = AppointmentLog.builder().eventType("BOOKED")
                .message("Booked").appointment(Appointment.builder().build()).build();
        AppointmentLog log2 = AppointmentLog.builder().eventType("CONFIRMED")
                .message("Confirmed").appointment(Appointment.builder().build()).build();

        Appointment appointment = Appointment.builder()
                .id(appointmentId).patient(patient).doctor(doctor)
                .slotStart(slotStart).slotEnd(slotStart.plusMinutes(30))
                .status("CONFIRMED").build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentLogRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId))
                .thenReturn(List.of(log1, log2));

        AppointmentResponse response = appointmentService.getById(appointmentId, patient);

        assertThat(response.getLogs()).hasSize(2);
        assertThat(response.getLogs()).extracting(l -> l.getEventType())
                .containsExactly("BOOKED", "CONFIRMED");
    }

    @Test
    void getById_anotherPatientsAppointment_throwsIllegalArgumentException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Appointment appointment = Appointment.builder()
                .id(appointmentId).patient(otherUser).doctor(doctor)
                .status("PENDING").build();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.getById(appointmentId, patient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access denied");
    }
}
