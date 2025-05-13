
package com.osbep.hospital.controller;

import com.osbep.hospital.model.Cita;
import com.osbep.hospital.repository.CitaRepository;
import com.osbep.hospital.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private CitaRepository citaRepo;

    @PostMapping
    public ResponseEntity<?> agendar(@RequestBody @Valid Cita cita) {
        try {
            return ResponseEntity.ok(citaService.agendarCita(cita));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public List<Cita> listar(@RequestParam Optional<LocalDate> fecha,
                             @RequestParam Optional<Long> doctorId,
                             @RequestParam Optional<Long> consultorioId) {
        return citaRepo.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        citaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Cita cita) {
        cita.setId(id);
        try {
            return ResponseEntity.ok(citaService.agendarCita(cita));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
