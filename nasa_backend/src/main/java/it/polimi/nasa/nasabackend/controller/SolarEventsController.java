package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.entity.CoronalMassEjection;
import it.polimi.nasa.nasabackend.entity.GeomagneticStorm;
import it.polimi.nasa.nasabackend.repository.CoronalMassEjectionRepository;
import it.polimi.nasa.nasabackend.repository.GeomagneticStormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solar-events")
@CrossOrigin(origins = "*")
public class SolarEventsController {

    @Autowired
    private CoronalMassEjectionRepository cmeRepository;

    @Autowired
    private GeomagneticStormRepository geomagneticStormRepository;

    // ========== CME ENDPOINTS ==========

    @GetMapping("/cme")
    public ResponseEntity<List<CoronalMassEjection>> getAllCme() {
        return ResponseEntity.ok(cmeRepository.findAll());
    }

    @GetMapping("/cme/date-range")
    public ResponseEntity<List<CoronalMassEjection>> getCmeByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(cmeRepository.findByStartTimeBetween(start, end));
    }

    @GetMapping("/cme/fast")
    public ResponseEntity<List<CoronalMassEjection>> getFastCme(
            @RequestParam(defaultValue = "500") double minSpeed) {
        // Conversione sicura per BigDecimal
        return ResponseEntity.ok(cmeRepository.findFastCme(BigDecimal.valueOf(minSpeed)));
    }

    // ========== GEOMAGNETIC STORM ENDPOINTS ==========

    @GetMapping("/storms")
    public ResponseEntity<List<GeomagneticStorm>> getAllStorms() {
        return ResponseEntity.ok(geomagneticStormRepository.findAll());
    }

    @GetMapping("/storms/date-range")
    public ResponseEntity<List<GeomagneticStorm>> getStormsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(geomagneticStormRepository.findByStartTimeBetween(start, end));
    }

    @GetMapping("/storms/major")
    public ResponseEntity<List<GeomagneticStorm>> getMajorStorms(
            @RequestParam(defaultValue = "5.0") double minKp) {
        return ResponseEntity.ok(geomagneticStormRepository.findMajorStorms(BigDecimal.valueOf(minKp)));
    }

    /**
     * NUOVO ENDPOINT: Filtra per Scala G (G1, G2, G3, G4, G5)
     * Richiede che nel Repository esista: List<GeomagneticStorm> findByGScaleGreaterThanEqual(Integer gScale);
     */
    @GetMapping("/storms/scale")
    public ResponseEntity<List<GeomagneticStorm>> getStormsByScale(
            @RequestParam(defaultValue = "1") int minGScale) {
        // Se non hai ancora il metodo nel repository, usa findAll e stream filter
        return ResponseEntity.ok(geomagneticStormRepository.findAll().stream()
                .filter(s -> s.getGScale() != null && s.getGScale() >= minGScale)
                .toList());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSolarEventsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCme", cmeRepository.count());
        stats.put("totalStorms", geomagneticStormRepository.count());

        // Count Fast CMEs (>1000 km/s)
        stats.put("fastCme", cmeRepository.findFastCme(BigDecimal.valueOf(1000)).size());

        // Count Major Storms (Kp >= 6, circa G2)
        stats.put("majorStorms", geomagneticStormRepository.findMajorStorms(BigDecimal.valueOf(6)).size());

        return ResponseEntity.ok(stats);
    }
}