
package com.osbep.hospital.repository;

import com.osbep.hospital.model.Cita;
import com.osbep.hospital.model.Consultorio;
import com.osbep.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByDoctorAndHorarioBetween(Doctor doctor, LocalDateTime start, LocalDateTime end);

    boolean existsByConsultorioAndHorario(Consultorio consultorio, LocalDateTime horario);

    boolean existsByDoctorAndHorario(Doctor doctor, LocalDateTime horario);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.doctor = :doctor AND DATE(c.horario) = :fecha")
    long countCitasPorDia(@Param("doctor") Doctor doctor, @Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE c.paciente = :paciente AND DATE(c.horario) = :fecha")
    List<Cita> citasPacienteDia(@Param("paciente") String paciente, @Param("fecha") LocalDate fecha);
}
