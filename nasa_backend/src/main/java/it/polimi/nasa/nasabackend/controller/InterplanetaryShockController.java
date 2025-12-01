package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.entity.InterplanetaryShock;
import it.polimi.nasa.nasabackend.service.InterplanetaryShockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ips")
@CrossOrigin(origins = "*")
public class InterplanetaryShockController {

    @Autowired
    private InterplanetaryShockService ipsService;

    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchIpsData(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // Ora il service usa il parsing date robusto, quindi non rischiamo crash
        List<InterplanetaryShock> ipsList = ipsService.fetchAndSaveIps(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("recordsFetched", ipsList.size());
        response.put("message", "Data fetched successfully using flexible date parser");
        response.put("data", ipsList);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InterplanetaryShock>> getAllIps() {
        return ResponseEntity.ok(ipsService.getAllIps());
    }

    @GetMapping("/earth")
    public ResponseEntity<List<InterplanetaryShock>> getEarthShocks() {
        // Ritorna gli shock con location = "Earth"
        return ResponseEntity.ok(ipsService.getEarthShocks());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getIpsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShocks", ipsService.getIpsCount());
        stats.put("earthShocks", ipsService.getEarthShocks().size());
        return ResponseEntity.ok(stats);
    }
}