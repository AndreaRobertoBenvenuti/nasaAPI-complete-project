package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.entity.NeoAsteroid;
import it.polimi.nasa.nasabackend.entity.NeoCloseApproach;
import it.polimi.nasa.nasabackend.repository.NeoAsteroidRepository;
import it.polimi.nasa.nasabackend.repository.NeoCloseApproachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/neo")
@CrossOrigin(origins = "*")
public class NeoController {

    @Autowired
    private NeoAsteroidRepository neoAsteroidRepository;

    @Autowired
    private NeoCloseApproachRepository neoCloseApproachRepository;

    @GetMapping("/asteroids")
    public ResponseEntity<List<NeoAsteroid>> getAllAsteroids() {
        return ResponseEntity.ok(neoAsteroidRepository.findAll());
    }

    @GetMapping("/asteroids/{id}")
    public ResponseEntity<NeoAsteroid> getAsteroidById(@PathVariable Long id) {
        return neoAsteroidRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/asteroids/hazardous")
    public ResponseEntity<List<NeoAsteroid>> getHazardousAsteroids() {
        return ResponseEntity.ok(neoAsteroidRepository.findByIsPotentiallyHazardous(true));
    }

    @GetMapping("/approaches")
    public ResponseEntity<List<NeoCloseApproach>> getAllApproaches() {
        return ResponseEntity.ok(neoCloseApproachRepository.findAll());
    }

    @GetMapping("/approaches/date-range")
    public ResponseEntity<List<NeoCloseApproach>> getApproachesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(neoCloseApproachRepository.findApproachesWithNeoByDateRange(start, end));
    }

    @GetMapping("/approaches/upcoming")
    public ResponseEntity<List<NeoCloseApproach>> getUpcomingApproaches() {
        return ResponseEntity.ok(neoCloseApproachRepository.findUpcomingHazardousApproaches(LocalDateTime.now()));
    }

    @GetMapping("/approaches/closest")
    public ResponseEntity<List<NeoCloseApproach>> getClosestApproaches(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(neoCloseApproachRepository.findClosestApproaches()
                .stream()
                .limit(limit)
                .toList());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNeoStats() {
        long totalAsteroids = neoAsteroidRepository.count();
        long hazardous = neoAsteroidRepository.findByIsPotentiallyHazardous(true).size();
        long totalApproaches = neoCloseApproachRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAsteroids", totalAsteroids);
        stats.put("hazardousAsteroids", hazardous);
        stats.put("safeAsteroids", totalAsteroids - hazardous);
        stats.put("totalApproaches", totalApproaches);

        return ResponseEntity.ok(stats);
    }
}