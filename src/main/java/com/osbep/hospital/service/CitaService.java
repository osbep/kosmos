
package com.osbep.hospital.service;

import com.osbep.hospital.model.Cita;
import com.osbep.hospital.repository.CitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
public class CitaService {

    @Autowired
    private CitaRepository citaRepo;

    public Cita agendarCita(Cita cita) {
        validarRestricciones(cita);
        return citaRepo.save(cita);
    }

    private void validarRestricciones(Cita cita) {
        LocalDate fecha = cita.getHorario().toLocalDate();

        if (citaRepo.existsByConsultorioAndHorario(cita.getConsultorio(), cita.getHorario())) {
            throw new IllegalArgumentException("Ya existe cita en este consultorio a esa hora.");
        }

        if (citaRepo.existsByDoctorAndHorario(cita.getDoctor(), cita.getHorario())) {
            throw new IllegalArgumentException("El doctor ya tiene cita a esa hora.");
        }

        long count = citaRepo.countCitasPorDia(cita.getDoctor(), fecha);
        if (count >= 8) {
            throw new IllegalArgumentException("El doctor no puede tener más de 8 citas por día.");
        }

        List<Cita> citasPaciente = citaRepo.citasPacienteDia(cita.getPaciente(), fecha);
        for (Cita existente : citasPaciente) {
            Duration diff = Duration.between(existente.getHorario(), cita.getHorario()).abs();
            if (diff.toHours() < 2) {
                throw new IllegalArgumentException("El paciente no puede tener citas con menos de 2 horas de diferencia.");
            }
        }
    }
}
