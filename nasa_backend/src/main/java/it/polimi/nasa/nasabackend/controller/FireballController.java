package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.dto.FireballDto;
import it.polimi.nasa.nasabackend.entity.Fireball;
import it.polimi.nasa.nasabackend.repository.FireballRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fireballs")
@CrossOrigin(origins = "*")
public class FireballController {

    @Autowired
    private FireballRepository fireballRepository;

    @GetMapping
    public ResponseEntity<List<FireballDto>> getAllFireballs() {
        List<Fireball> fireballs = fireballRepository.findAll();
        List<FireballDto> dtos = fireballs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FireballDto> getFireballById(@PathVariable Long id) {
        return fireballRepository.findById(id)
                .map(fireball -> ResponseEntity.ok(convertToDto(fireball)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<FireballDto>> getFireballsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<Fireball> fireballs = fireballRepository.findByEventDateBetween(start, end);
        List<FireballDto> dtos = fireballs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/with-location")
    public ResponseEntity<List<FireballDto>> getFireballsWithLocation() {
        List<Fireball> fireballs = fireballRepository.findAllWithLocation();
        List<FireballDto> dtos = fireballs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/top-energy")
    public ResponseEntity<List<FireballDto>> getTopEnergyFireballs(
            @RequestParam(defaultValue = "10") int limit) {

        List<Fireball> fireballs = fireballRepository.findTopByEnergyDesc();
        List<FireballDto> dtos = fireballs.stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFireballStats() {
        long total = fireballRepository.count();
        long withLocation = fireballRepository.findAllWithLocation().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("withLocation", withLocation);
        stats.put("withoutLocation", total - withLocation);

        return ResponseEntity.ok(stats);
    }

    // Helper method
    private FireballDto convertToDto(Fireball fireball) {
        // NOTA: Abbiamo aggiunto vx, vy, vz all'Entity.
        // Se vuoi esporli, aggiungili alla classe FireballDto e passali qui.
        return new FireballDto(
                fireball.getId(),
                fireball.getEventDate(),
                fireball.getLatitude(),
                fireball.getLongitude(),
                fireball.getAltitudeKm(),
                fireball.getTotalImpactEnergyKt(),
                fireball.getVelocityKmS()
        );
    }
}