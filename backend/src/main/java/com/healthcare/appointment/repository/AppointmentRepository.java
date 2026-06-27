package com.healthcare.appointment.repository;

import com.healthcare.appointment.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdOrderBySlotStartDesc(UUID patientId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.slotStart = :slotStart AND a.status != 'CANCELLED'")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Appointment> findActiveByDoctorAndSlot(@Param("doctorId") UUID doctorId,
                                                     @Param("slotStart") OffsetDateTime slotStart);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.slotStart >= :dayStart AND a.slotStart < :dayEnd " +
           "AND a.status != 'CANCELLED'")
    List<Appointment> findActiveByDoctorAndDate(@Param("doctorId") UUID doctorId,
                                                 @Param("dayStart") OffsetDateTime dayStart,
                                                 @Param("dayEnd") OffsetDateTime dayEnd);
}
