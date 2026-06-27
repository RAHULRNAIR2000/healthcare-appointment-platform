package com.healthcare.appointment.repository;

import com.healthcare.appointment.entity.AppointmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AppointmentLogRepository extends JpaRepository<AppointmentLog, UUID> {
    List<AppointmentLog> findByAppointmentIdOrderByCreatedAtAsc(UUID appointmentId);
}
