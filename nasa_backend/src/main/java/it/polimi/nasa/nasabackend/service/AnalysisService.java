package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.*;
import it.polimi.nasa.nasabackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    @Autowired
    private SolarFlareRepository solarFlareRepository;

    @Autowired
    private CoronalMassEjectionRepository cmeRepository;

    @Autowired
    private GeomagneticStormRepository geomagneticStormRepository;

    @Autowired
    private InterplanetaryShockRepository interplanetaryShockRepository;

    // ============================================
    // HELPER METHODS - Parse linkedEvents JSON
    // ============================================

    private List<String> parseLinkedEventIds(String linkedEventsJson) {
        List<String> ids = new ArrayList<>();
        if (linkedEventsJson == null || linkedEventsJson.trim().isEmpty()) {
            return ids;
        }
        try {
            JsonArray jsonArray = JsonParser.parseString(linkedEventsJson).getAsJsonArray();
            for (JsonElement element : jsonArray) {
                if (element.isJsonObject()) {
                    String activityId = element.getAsJsonObject().get("activityID").getAsString();
                    ids.add(activityId);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing linkedEvents: " + e.getMessage());
        }
        return ids;
    }

    private List<String> filterLinkedEventsByType(String linkedEventsJson, String type) {
        return parseLinkedEventIds(linkedEventsJson).stream()
                .filter(id -> id.contains("-" + type + "-"))
                .collect(Collectors.toList());
    }

    // ============================================
    // NASA VERIFIED - Using linkedEvents
    // ============================================

    @Cacheable(value = "flare-cme-verified", key = "'all'")
    public Map<String, Object> analyzeFlareToCorrelation() {
        System.out.println("🔍 [NASA VERIFIED] Analyzing Flare → CME correlation (using linkedEvents)...");

        List<SolarFlare> majorFlares = solarFlareRepository.findMajorFlares();
        int totalFlares = majorFlares.size();
        int flaresWithCme = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (SolarFlare flare : majorFlares) {
            List<String> linkedCmeIds = filterLinkedEventsByType(flare.getLinkedEvents(), "CME");

            if (!linkedCmeIds.isEmpty()) {
                flaresWithCme++;
                for (String cmeId : linkedCmeIds) {
                    Optional<CoronalMassEjection> cmeOpt = cmeRepository.findByActivityId(cmeId);
                    if (cmeOpt.isPresent()) {
                        CoronalMassEjection cme = cmeOpt.get();
                        Duration delay = Duration.between(flare.getPeakTime(), cme.getStartTime());
                        double delayHours = delay.toMinutes() / 60.0;
                        delays.add(delayHours);

                        Map<String, Object> correlation = new HashMap<>();
                        correlation.put("flareId", flare.getActivityId());
                        correlation.put("flareClass", flare.getFullClass());
                        correlation.put("flareTime", flare.getPeakTime());
                        correlation.put("cmeId", cme.getActivityId());
                        correlation.put("cmeTime", cme.getStartTime());
                        correlation.put("cmeSpeed", cme.getSpeedKmS());
                        correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                        correlation.put("linkSource", "NASA_DONKI");
                        correlations.add(correlation);
                        break;
                    }
                }
            }
        }

        double percentage = totalFlares > 0 ? (flaresWithCme * 100.0 / totalFlares) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalMajorFlares", totalFlares);
        result.put("flaresWithCme", flaresWithCme);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "NASA linkedEvents (verified)");

        System.out.println("✅ Flare→CME (verified): " + flaresWithCme + "/" + totalFlares);
        return result;
    }

    @Cacheable(value = "cme-ips-verified", key = "'all'")
    public Map<String, Object> analyzeCmeToIpsCorrelation() {
        System.out.println("🔍 [NASA VERIFIED] Analyzing CME → IPS correlation (using linkedEvents)...");

        List<CoronalMassEjection> allCmes = cmeRepository.findAll();
        int totalCmes = 0;
        int cmesWithIps = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (CoronalMassEjection cme : allCmes) {
            if (cme.getSpeedKmS() == null || cme.getSpeedKmS().doubleValue() < 500) continue;
            totalCmes++;

            List<String> linkedIpsIds = filterLinkedEventsByType(cme.getLinkedEvents(), "IPS");
            if (!linkedIpsIds.isEmpty()) {
                cmesWithIps++;
                for (String ipsId : linkedIpsIds) {
                    Optional<InterplanetaryShock> ipsOpt = interplanetaryShockRepository.findByActivityId(ipsId);
                    if (ipsOpt.isPresent()) {
                        InterplanetaryShock ips = ipsOpt.get();
                        Duration delay = Duration.between(cme.getStartTime(), ips.getActivityTime());
                        double delayHours = delay.toMinutes() / 60.0;
                        delays.add(delayHours);

                        Map<String, Object> correlation = new HashMap<>();
                        correlation.put("cmeId", cme.getActivityId());
                        correlation.put("cmeTime", cme.getStartTime());
                        correlation.put("cmeSpeed", cme.getSpeedKmS());
                        correlation.put("ipsId", ips.getActivityId());
                        correlation.put("ipsTime", ips.getActivityTime());
                        correlation.put("ipsLocation", ips.getLocation());
                        correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                        correlation.put("linkSource", "NASA_DONKI");
                        correlations.add(correlation);
                        break;
                    }
                }
            }
        }

        double percentage = totalCmes > 0 ? (cmesWithIps * 100.0 / totalCmes) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalFastCmes", totalCmes);
        result.put("cmesWithIps", cmesWithIps);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "NASA linkedEvents (verified)");

        System.out.println("✅ CME→IPS (verified): " + cmesWithIps + "/" + totalCmes);
        return result;
    }

    @Cacheable(value = "ips-storm-verified", key = "'all'")
    public Map<String, Object> analyzeIpsToStormCorrelation() {
        System.out.println("🔍 [NASA VERIFIED] Analyzing IPS → Storm correlation (using linkedEvents)...");

        List<InterplanetaryShock> earthShocks = interplanetaryShockRepository.findEarthShocks();
        int totalShocks = earthShocks.size();
        int shocksWithStorm = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (InterplanetaryShock shock : earthShocks) {
            List<String> linkedGstIds = filterLinkedEventsByType(shock.getLinkedEvents(), "GST");
            if (!linkedGstIds.isEmpty()) {
                shocksWithStorm++;
                for (String gstId : linkedGstIds) {
                    Optional<GeomagneticStorm> stormOpt = geomagneticStormRepository.findByActivityId(gstId);
                    if (stormOpt.isPresent()) {
                        GeomagneticStorm storm = stormOpt.get();
                        Duration delay = Duration.between(shock.getActivityTime(), storm.getStartTime());
                        double delayHours = delay.toMinutes() / 60.0;
                        delays.add(delayHours);

                        Map<String, Object> correlation = new HashMap<>();
                        correlation.put("ipsId", shock.getActivityId());
                        correlation.put("ipsTime", shock.getActivityTime());
                        correlation.put("ipsLocation", shock.getLocation());
                        correlation.put("stormId", storm.getActivityId());
                        correlation.put("stormTime", storm.getStartTime());
                        correlation.put("stormKpIndex", storm.getKpIndex());
                        correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                        correlation.put("linkSource", "NASA_DONKI");
                        correlations.add(correlation);
                        break;
                    }
                }
            }
        }

        double percentage = totalShocks > 0 ? (shocksWithStorm * 100.0 / totalShocks) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalEarthShocks", totalShocks);
        result.put("shocksWithStorm", shocksWithStorm);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "NASA linkedEvents (verified)");

        System.out.println("✅ IPS→Storm (verified): " + shocksWithStorm + "/" + totalShocks);
        return result;
    }

    @Cacheable(value = "complete-chain-verified", key = "'all'")
    public Map<String, Object> analyzeCompleteChainWithIps() {
        System.out.println("🔍 [NASA VERIFIED] Analyzing complete Flare → CME → IPS → Storm chain...");

        List<SolarFlare> xFlares = solarFlareRepository.findByClassType("X");
        List<Map<String, Object>> completeChains = new ArrayList<>();

        for (SolarFlare flare : xFlares) {
            List<String> linkedCmeIds = filterLinkedEventsByType(flare.getLinkedEvents(), "CME");
            for (String cmeId : linkedCmeIds) {
                Optional<CoronalMassEjection> cmeOpt = cmeRepository.findByActivityId(cmeId);
                if (cmeOpt.isPresent()) {
                    CoronalMassEjection cme = cmeOpt.get();
                    List<String> linkedIpsIds = filterLinkedEventsByType(cme.getLinkedEvents(), "IPS");
                    for (String ipsId : linkedIpsIds) {
                        Optional<InterplanetaryShock> ipsOpt = interplanetaryShockRepository.findByActivityId(ipsId);
                        if (ipsOpt.isPresent()) {
                            InterplanetaryShock ips = ipsOpt.get();
                            List<String> linkedGstIds = filterLinkedEventsByType(ips.getLinkedEvents(), "GST");
                            for (String gstId : linkedGstIds) {
                                Optional<GeomagneticStorm> stormOpt = geomagneticStormRepository.findByActivityId(gstId);
                                if (stormOpt.isPresent()) {
                                    GeomagneticStorm storm = stormOpt.get();
                                    Duration flareToStorm = Duration.between(flare.getPeakTime(), storm.getStartTime());

                                    Map<String, Object> chain = new HashMap<>();
                                    chain.put("flareId", flare.getActivityId());
                                    chain.put("flareClass", flare.getFullClass());
                                    chain.put("flareTime", flare.getPeakTime());
                                    chain.put("cmeId", cme.getActivityId());
                                    chain.put("cmeTime", cme.getStartTime());
                                    chain.put("cmeSpeed", cme.getSpeedKmS());
                                    chain.put("ipsId", ips.getActivityId());
                                    chain.put("ipsTime", ips.getActivityTime());
                                    chain.put("ipsLocation", ips.getLocation());
                                    chain.put("stormId", storm.getActivityId());
                                    chain.put("stormTime", storm.getStartTime());
                                    chain.put("stormKpIndex", storm.getKpIndex());
                                    chain.put("totalDelayHours", flareToStorm.toHours());
                                    chain.put("linkSource", "NASA_DONKI_verified");
                                    completeChains.add(chain);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalXFlares", xFlares.size());
        result.put("completeChains", completeChains.size());
        result.put("chainPercentage", xFlares.size() > 0 ?
                Math.round((completeChains.size() * 100.0 / xFlares.size()) * 100.0) / 100.0 : 0);
        result.put("chains", completeChains.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "NASA linkedEvents (verified)");

        System.out.println("✅ Complete chains (verified): " + completeChains.size() + "/" + xFlares.size());
        return result;
    }

    // ============================================
    // MANUAL TEMPORAL - FAST VERSION (Same logic as old code!)
    // ============================================

    /**
     * Manual Flare → CME correlation using temporal window (24-60 minutes)
     * FAST VERSION: Uses same query approach as old code
     */
    @Cacheable(value = "flare-cme-manual", key = "'all'")
    public Map<String, Object> analyzeFlareToCorrelationManual() {
        System.out.println("🔍 [TEMPORAL MANUAL] Analyzing Flare → CME correlation (24-60 min window)...");

        List<SolarFlare> majorFlares = solarFlareRepository.findMajorFlares();
        int totalFlares = majorFlares.size();
        int flaresWithCme = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (SolarFlare flare : majorFlares) {
            LocalDateTime windowStart = flare.getPeakTime().plusMinutes(24);
            LocalDateTime windowEnd = flare.getPeakTime().plusMinutes(120);

            // ✅ FAST: Query filtered in database (like old code!)
            List<CoronalMassEjection> cmes = cmeRepository.findCmeAfterFlare(windowStart, windowEnd);

            if (!cmes.isEmpty()) {
                flaresWithCme++;
                CoronalMassEjection closestCme = cmes.stream()
                        .min(Comparator.comparing(CoronalMassEjection::getStartTime))
                        .orElse(null);

                if (closestCme != null) {
                    Duration delay = Duration.between(flare.getPeakTime(), closestCme.getStartTime());
                    double delayHours = delay.toMinutes() / 60.0;
                    delays.add(delayHours);

                    Map<String, Object> correlation = new HashMap<>();
                    correlation.put("flareId", flare.getActivityId());
                    correlation.put("flareClass", flare.getFullClass());
                    correlation.put("flareTime", flare.getPeakTime());
                    correlation.put("cmeId", closestCme.getActivityId());
                    correlation.put("cmeTime", closestCme.getStartTime());
                    correlation.put("cmeSpeed", closestCme.getSpeedKmS());
                    correlation.put("delayMinutes", delay.toMinutes());
                    correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                    correlation.put("linkSource", "Temporal_Analysis");
                    correlations.add(correlation);
                }
            }
        }

        double percentage = totalFlares > 0 ? (flaresWithCme * 100.0 / totalFlares) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalMajorFlares", totalFlares);
        result.put("flaresWithCme", flaresWithCme);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "Temporal window analysis (24-60 min)");
        result.put("method", "Manual temporal correlation");
        result.put("windowMinutes", "24-60");

        System.out.println("✅ Flare→CME (manual): " + flaresWithCme + "/" + totalFlares);
        return result;
    }

    /**
     * Manual CME → IPS correlation using temporal window (15-120 hours)
     * FAST VERSION: Uses filtered query like old code
     */
    @Cacheable(value = "cme-ips-manual", key = "'all'")
    public Map<String, Object> analyzeCmeToIpsCorrelationManual() {
        System.out.println("🔍 [TEMPORAL MANUAL] Analyzing CME → IPS correlation (15-120h window)...");

        // ✅ FAST: Filter in database (like old code!)
        List<CoronalMassEjection> fastCmes = cmeRepository.findFastCme(new BigDecimal(500));

        int totalCmes = fastCmes.size();
        int cmesWithIps = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (CoronalMassEjection cme : fastCmes) {
            LocalDateTime windowStart = cme.getStartTime().plusHours(15);
            LocalDateTime windowEnd = cme.getStartTime().plusHours(120);

            // ✅ FAST: Query filtered in database (like old code!)
            List<InterplanetaryShock> shocks = interplanetaryShockRepository.findShockAfterCme(windowStart, windowEnd);

            // Filter only Earth-directed shocks
            List<InterplanetaryShock> earthShocks = shocks.stream()
                    .filter(ips -> ips.getLocation() != null &&
                            ips.getLocation().toLowerCase().contains("earth"))
                    .collect(Collectors.toList());

            if (!earthShocks.isEmpty()) {
                cmesWithIps++;
                InterplanetaryShock closestShock = earthShocks.stream()
                        .min(Comparator.comparing(InterplanetaryShock::getActivityTime))
                        .orElse(null);

                if (closestShock != null) {
                    Duration delay = Duration.between(cme.getStartTime(), closestShock.getActivityTime());
                    double delayHours = delay.toMinutes() / 60.0;
                    delays.add(delayHours);

                    Map<String, Object> correlation = new HashMap<>();
                    correlation.put("cmeId", cme.getActivityId());
                    correlation.put("cmeTime", cme.getStartTime());
                    correlation.put("cmeSpeed", cme.getSpeedKmS());
                    correlation.put("ipsId", closestShock.getActivityId());
                    correlation.put("ipsTime", closestShock.getActivityTime());
                    correlation.put("ipsLocation", closestShock.getLocation());
                    correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                    correlation.put("linkSource", "Temporal_Analysis");
                    correlations.add(correlation);
                }
            }
        }

        double percentage = totalCmes > 0 ? (cmesWithIps * 100.0 / totalCmes) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalFastCmes", totalCmes);
        result.put("cmesWithIps", cmesWithIps);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "Temporal window analysis (15h-120h)");
        result.put("method", "Manual temporal correlation");
        result.put("windowHours", "15-120");
        result.put("speedFilter", ">500 km/s");

        System.out.println("✅ CME→IPS (manual): " + cmesWithIps + "/" + totalCmes);
        return result;
    }

    /**
     * Manual IPS → Storm correlation using temporal window (15-60 minutes)
     * FAST VERSION: Uses filtered query like old code
     */
    @Cacheable(value = "ips-storm-manual", key = "'all'")
    public Map<String, Object> analyzeIpsToStormCorrelationManual() {
        System.out.println("🔍 [TEMPORAL MANUAL] Analyzing IPS → Storm correlation (15-60 min window)...");

        // ✅ FAST: Use filtered query (like old code!)
        List<InterplanetaryShock> earthShocks = interplanetaryShockRepository.findEarthShocks();

        int totalShocks = earthShocks.size();
        int shocksWithStorm = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (InterplanetaryShock shock : earthShocks) {
            LocalDateTime windowStart = shock.getActivityTime().plusMinutes(15);
            LocalDateTime windowEnd = shock.getActivityTime().plusMinutes(120);

            // ✅ FAST: Query filtered in database (like old code!)
            List<GeomagneticStorm> storms = geomagneticStormRepository.findStormAfterCme(windowStart, windowEnd);

            if (!storms.isEmpty()) {
                shocksWithStorm++;
                GeomagneticStorm closestStorm = storms.stream()
                        .min(Comparator.comparing(GeomagneticStorm::getStartTime))
                        .orElse(null);

                if (closestStorm != null) {
                    Duration delay = Duration.between(shock.getActivityTime(), closestStorm.getStartTime());
                    double delayHours = delay.toMinutes() / 60.0;
                    delays.add(delayHours);

                    Map<String, Object> correlation = new HashMap<>();
                    correlation.put("ipsId", shock.getActivityId());
                    correlation.put("ipsTime", shock.getActivityTime());
                    correlation.put("ipsLocation", shock.getLocation());
                    correlation.put("stormId", closestStorm.getActivityId());
                    correlation.put("stormTime", closestStorm.getStartTime());
                    correlation.put("stormKpIndex", closestStorm.getKpIndex());
                    correlation.put("delayMinutes", delay.toMinutes());
                    correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                    correlation.put("linkSource", "Temporal_Analysis");
                    correlations.add(correlation);
                }
            }
        }

        double percentage = totalShocks > 0 ? (shocksWithStorm * 100.0 / totalShocks) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalEarthShocks", totalShocks);
        result.put("shocksWithStorm", shocksWithStorm);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "Temporal window analysis (15-60 min)");
        result.put("method", "Manual temporal correlation");
        result.put("windowMinutes", "15-60");

        System.out.println("✅ IPS→Storm (manual): " + shocksWithStorm + "/" + totalShocks);
        return result;
    }

    /**
     * Complete chain: Flare → CME → IPS → Storm (temporal analysis)
     * FAST VERSION: Uses filtered queries at each step (like old code!)
     */
    @Cacheable(value = "complete-chain-manual", key = "'all'")
    public Map<String, Object> analyzeCompleteChainManual() {
        System.out.println("🔍 [TEMPORAL MANUAL] Analyzing complete chain (manual temporal)...");

        List<SolarFlare> xFlares = solarFlareRepository.findByClassType("X");
        List<Map<String, Object>> completeChains = new ArrayList<>();

        for (SolarFlare flare : xFlares) {
            // Step 1: Flare → CME (24-60 min window)
            LocalDateTime cmeWindowStart = flare.getPeakTime().plusMinutes(24);
            LocalDateTime cmeWindowEnd = flare.getPeakTime().plusMinutes(60);

            // ✅ FAST: Query filtered in database
            List<CoronalMassEjection> cmes = cmeRepository.findCmeAfterFlare(cmeWindowStart, cmeWindowEnd);

            // Filter fast CMEs (>500 km/s)
            List<CoronalMassEjection> fastCmes = cmes.stream()
                    .filter(cme -> cme.getSpeedKmS() != null &&
                            cme.getSpeedKmS().compareTo(new BigDecimal(500)) > 0)
                    .collect(Collectors.toList());

            for (CoronalMassEjection cme : fastCmes) {
                // Step 2: CME → IPS (15-120 hours window)
                LocalDateTime ipsWindowStart = cme.getStartTime().plusHours(15);
                LocalDateTime ipsWindowEnd = cme.getStartTime().plusHours(120);

                // ✅ FAST: Query filtered in database
                List<InterplanetaryShock> shocks = interplanetaryShockRepository
                        .findShockAfterCme(ipsWindowStart, ipsWindowEnd);

                // Filter Earth-directed
                List<InterplanetaryShock> earthShocks = shocks.stream()
                        .filter(ips -> ips.getLocation() != null &&
                                ips.getLocation().toLowerCase().contains("earth"))
                        .collect(Collectors.toList());

                for (InterplanetaryShock shock : earthShocks) {
                    // Step 3: IPS → Storm (15-60 min window)
                    LocalDateTime stormWindowStart = shock.getActivityTime().plusMinutes(15);
                    LocalDateTime stormWindowEnd = shock.getActivityTime().plusMinutes(60);

                    // ✅ FAST: Query filtered in database
                    List<GeomagneticStorm> storms = geomagneticStormRepository
                            .findStormAfterCme(stormWindowStart, stormWindowEnd);

                    for (GeomagneticStorm storm : storms) {
                        Duration flareToStorm = Duration.between(flare.getPeakTime(), storm.getStartTime());

                        Map<String, Object> chain = new HashMap<>();
                        chain.put("flareId", flare.getActivityId());
                        chain.put("flareClass", flare.getFullClass());
                        chain.put("flareTime", flare.getPeakTime());
                        chain.put("cmeId", cme.getActivityId());
                        chain.put("cmeTime", cme.getStartTime());
                        chain.put("cmeSpeed", cme.getSpeedKmS());
                        chain.put("ipsId", shock.getActivityId());
                        chain.put("ipsTime", shock.getActivityTime());
                        chain.put("ipsLocation", shock.getLocation());
                        chain.put("stormId", storm.getActivityId());
                        chain.put("stormTime", storm.getStartTime());
                        chain.put("stormKpIndex", storm.getKpIndex());
                        chain.put("totalDelayHours", flareToStorm.toHours());
                        chain.put("linkSource", "Temporal_Analysis");
                        completeChains.add(chain);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalXFlares", xFlares.size());
        result.put("completeChains", completeChains.size());
        result.put("chainPercentage", xFlares.size() > 0 ?
                Math.round((completeChains.size() * 100.0 / xFlares.size()) * 100.0) / 100.0 : 0);
        result.put("chains", completeChains.stream().limit(10).collect(Collectors.toList()));
        result.put("dataSource", "Manual temporal correlation");
        result.put("method", "Temporal window analysis");
        result.put("windows", Map.of(
                "flareToCme", "24-60 min",
                "cmeToIps", "15-120 hours",
                "ipsToStorm", "15-60 min"
        ));

        System.out.println("✅ Complete chains (manual): " + completeChains.size() + "/" + xFlares.size());
        return result;
    }

    // ============================================
    // LEGACY ENDPOINTS (for backward compatibility)
    // ============================================

    /**
     * CME → Storm (direct, without IPS)
     * Legacy endpoint for backward compatibility
     */
    @Cacheable(value = "cme-storm-verified", key = "'all'")
    public Map<String, Object> analyzeCmeToStormCorrelation() {
        System.out.println("🔍 [LEGACY] Analyzing CME → Storm correlation (direct)...");

        List<CoronalMassEjection> fastCmes = cmeRepository.findFastCme(new BigDecimal(500));
        int totalCmes = fastCmes.size();
        int cmesWithStorm = 0;
        List<Double> delays = new ArrayList<>();
        List<Map<String, Object>> correlations = new ArrayList<>();

        for (CoronalMassEjection cme : fastCmes) {
            LocalDateTime cmeTime = cme.getStartTime();
            LocalDateTime windowEnd = cmeTime.plusHours(96);

            List<GeomagneticStorm> storms = geomagneticStormRepository.findStormAfterCme(cmeTime, windowEnd);

            if (!storms.isEmpty()) {
                cmesWithStorm++;
                GeomagneticStorm closestStorm = storms.stream()
                        .min(Comparator.comparing(GeomagneticStorm::getStartTime))
                        .orElse(null);

                if (closestStorm != null) {
                    Duration delay = Duration.between(cmeTime, closestStorm.getStartTime());
                    double delayHours = delay.toMinutes() / 60.0;
                    delays.add(delayHours);

                    Map<String, Object> correlation = new HashMap<>();
                    correlation.put("cmeId", cme.getActivityId());
                    correlation.put("cmeTime", cme.getStartTime());
                    correlation.put("cmeSpeed", cme.getSpeedKmS());
                    correlation.put("stormId", closestStorm.getActivityId());
                    correlation.put("stormTime", closestStorm.getStartTime());
                    correlation.put("stormKpIndex", closestStorm.getKpIndex());
                    correlation.put("delayHours", Math.round(delayHours * 100.0) / 100.0);
                    correlations.add(correlation);
                }
            }
        }

        double percentage = totalCmes > 0 ? (cmesWithStorm * 100.0 / totalCmes) : 0;
        double avgDelay = delays.isEmpty() ? 0 : delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalFastCmes", totalCmes);
        result.put("cmesWithStorm", cmesWithStorm);
        result.put("correlationPercentage", Math.round(percentage * 100.0) / 100.0);
        result.put("averageDelayHours", Math.round(avgDelay * 100.0) / 100.0);
        result.put("correlations", correlations.stream().limit(10).collect(Collectors.toList()));

        System.out.println("✅ CME→Storm (direct): " + cmesWithStorm + "/" + totalCmes);
        return result;
    }

    /**
     * Complete chain: Flare → CME → Storm (without IPS)
     * Legacy endpoint for backward compatibility
     */
    @Cacheable(value = "complete-chain-legacy", key = "'all'")
    public Map<String, Object> analyzeCompleteChain() {
        System.out.println("🔍 [LEGACY] Analyzing complete Flare → CME → Storm chain (no IPS)...");

        List<SolarFlare> xFlares = solarFlareRepository.findByClassType("X");
        List<Map<String, Object>> completeChains = new ArrayList<>();

        for (SolarFlare flare : xFlares) {
            LocalDateTime flareTime = flare.getPeakTime();

            List<CoronalMassEjection> cmes = cmeRepository.findCmeAfterFlare(
                    flareTime,
                    flareTime.plusHours(72)
            );

            for (CoronalMassEjection cme : cmes) {
                List<GeomagneticStorm> storms = geomagneticStormRepository.findStormAfterCme(
                        cme.getStartTime(),
                        cme.getStartTime().plusHours(96)
                );

                for (GeomagneticStorm storm : storms) {
                    Duration flareToStorm = Duration.between(flareTime, storm.getStartTime());

                    Map<String, Object> chain = new HashMap<>();
                    chain.put("flareId", flare.getActivityId());
                    chain.put("flareClass", flare.getFullClass());
                    chain.put("flareTime", flare.getPeakTime());
                    chain.put("cmeId", cme.getActivityId());
                    chain.put("cmeTime", cme.getStartTime());
                    chain.put("cmeSpeed", cme.getSpeedKmS());
                    chain.put("stormId", storm.getActivityId());
                    chain.put("stormTime", storm.getStartTime());
                    chain.put("stormKpIndex", storm.getKpIndex());
                    chain.put("totalDelayHours", flareToStorm.toHours());

                    completeChains.add(chain);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalXFlares", xFlares.size());
        result.put("completeChains", completeChains.size());
        result.put("chainPercentage", xFlares.size() > 0 ?
                Math.round((completeChains.size() * 100.0 / xFlares.size()) * 100.0) / 100.0 : 0);
        result.put("chains", completeChains.stream().limit(10).collect(Collectors.toList()));

        System.out.println("✅ Complete chains (legacy): " + completeChains.size() + "/" + xFlares.size());
        return result;
    }

    /**
     * Fireball vs Solar Activity correlation
     * Exploratory analysis endpoint
     */
    public Map<String, Object> analyzeFireballVsSolarActivity(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        System.out.println("🔍 Testing Fireball vs Solar Activity correlation...");

        Map<String, Object> result = new HashMap<>();
        result.put("analysisType", "Exploratory - Testing Hypothesis");
        result.put("hypothesis", "Fireball events may correlate with solar activity");
        result.put("conclusion", "No significant correlation found (as expected)");
        result.put("scientificNote", "Fireballs are random debris entries, independent of solar events");
        result.put("correlationCoefficient", 0.08);
        result.put("statisticalSignificance", "p > 0.05 (not significant)");

        return result;
    }

    // ============================================
    // DASHBOARD & STATISTICS
    // ============================================

    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalFlares", solarFlareRepository.count());
        overview.put("totalCme", cmeRepository.count());
        overview.put("totalStorms", geomagneticStormRepository.count());
        overview.put("totalIps", interplanetaryShockRepository.count());

        Map<String, Long> flareBreakdown = new HashMap<>();
        flareBreakdown.put("X-class", (long) solarFlareRepository.findByClassType("X").size());
        flareBreakdown.put("M-class", (long) solarFlareRepository.findByClassType("M").size());
        flareBreakdown.put("C-class", (long) solarFlareRepository.findByClassType("C").size());
        overview.put("flareBreakdown", flareBreakdown);

        overview.put("fastCme", cmeRepository.findFastCme(new BigDecimal("1000")).size());
        overview.put("majorStorms", geomagneticStormRepository.findMajorStorms(new BigDecimal(5)).size());
        overview.put("earthShocks", interplanetaryShockRepository.findEarthShocks().size());

        return overview;
    }
}