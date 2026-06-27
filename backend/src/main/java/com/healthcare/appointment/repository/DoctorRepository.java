package com.healthcare.appointment.repository;

import com.healthcare.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
}
