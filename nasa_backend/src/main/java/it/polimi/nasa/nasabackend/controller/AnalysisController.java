package it.polimi.nasa.nasabackend.controller;

import it.polimi.nasa.nasabackend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    // ============================================
    // NASA VERIFIED CORRELATIONS (linkedEvents)
    // ============================================

    @GetMapping("/flare-cme")
    public ResponseEntity<Map<String, Object>> analyzeFlareToCorrelation() {
        return ResponseEntity.ok(analysisService.analyzeFlareToCorrelation());
    }

    @GetMapping("/cme-ips")
    public ResponseEntity<Map<String, Object>> analyzeCmeToIpsCorrelation() {
        return ResponseEntity.ok(analysisService.analyzeCmeToIpsCorrelation());
    }

    @GetMapping("/ips-storm")
    public ResponseEntity<Map<String, Object>> analyzeIpsToStormCorrelation() {
        return ResponseEntity.ok(analysisService.analyzeIpsToStormCorrelation());
    }

    @GetMapping("/cme-storm")
    public ResponseEntity<Map<String, Object>> analyzeCmeToStormCorrelation() {
        return ResponseEntity.ok(analysisService.analyzeCmeToStormCorrelation());
    }

    @GetMapping("/complete-chain")
    public ResponseEntity<Map<String, Object>> analyzeCompleteChain() {
        return ResponseEntity.ok(analysisService.analyzeCompleteChain());
    }

    @GetMapping("/complete-chain-ips")
    public ResponseEntity<Map<String, Object>> analyzeCompleteChainWithIps() {
        return ResponseEntity.ok(analysisService.analyzeCompleteChainWithIps());
    }

    // ============================================
    // MANUAL TEMPORAL CORRELATIONS
    // ============================================

    @GetMapping("/manual/flare-cme")
    public ResponseEntity<Map<String, Object>> analyzeFlareToCorrelationManual() {
        return ResponseEntity.ok(analysisService.analyzeFlareToCorrelationManual());
    }

    @GetMapping("/manual/cme-ips")
    public ResponseEntity<Map<String, Object>> analyzeCmeToIpsCorrelationManual() {
        return ResponseEntity.ok(analysisService.analyzeCmeToIpsCorrelationManual());
    }

    @GetMapping("/manual/ips-storm")
    public ResponseEntity<Map<String, Object>> analyzeIpsToStormCorrelationManual() {
        return ResponseEntity.ok(analysisService.analyzeIpsToStormCorrelationManual());
    }

    @GetMapping("/manual/complete-chain")
    public ResponseEntity<Map<String, Object>> analyzeCompleteChainManual() {
        return ResponseEntity.ok(analysisService.analyzeCompleteChainManual());
    }

    // ============================================
    // OTHER ENDPOINTS
    // ============================================

    @GetMapping("/fireball-solar")
    public ResponseEntity<Map<String, Object>> analyzeFireballVsSolar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(analysisService.analyzeFireballVsSolarActivity(start, end));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        return ResponseEntity.ok(analysisService.getDashboardOverview());
    }
}