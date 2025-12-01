package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.entity.SolarFlare;
import it.polimi.nasa.nasabackend.repository.SolarFlareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solar-flares")
@CrossOrigin(origins = "*")
public class SolarFlareController {

    @Autowired
    private SolarFlareRepository solarFlareRepository;

    @GetMapping
    public ResponseEntity<List<SolarFlare>> getAllFlares() {
        return ResponseEntity.ok(solarFlareRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolarFlare> getFlareById(@PathVariable Long id) {
        return solarFlareRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<SolarFlare>> getFlaresByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(solarFlareRepository.findByPeakTimeBetween(start, end));
    }

    /**
     * GET /api/solar-flares/class/{classType}
     * classType: B, C, M, X
     */
    @GetMapping("/class/{classType}")
    public ResponseEntity<List<SolarFlare>> getFlaresByClass(@PathVariable String classType) {
        // Il service ora salva "X" pulito nel campo classType, quindi la query funzionerà perfettamente
        return ResponseEntity.ok(solarFlareRepository.findByClassType(classType.toUpperCase()));
    }

    @GetMapping("/major")
    public ResponseEntity<List<SolarFlare>> getMajorFlares() {
        return ResponseEntity.ok(solarFlareRepository.findMajorFlares());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFlareStats() {
        long total = solarFlareRepository.count();
        // Queste query contano basandosi sul campo classType (es. 'X') che abbiamo parsato
        long classX = solarFlareRepository.findByClassType("X").size();
        long classM = solarFlareRepository.findByClassType("M").size();
        long classC = solarFlareRepository.findByClassType("C").size();
        long classB = solarFlareRepository.findByClassType("B").size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("classX", classX);
        stats.put("classM", classM);
        stats.put("classC", classC);
        stats.put("classB", classB);

        return ResponseEntity.ok(stats);
    }
}