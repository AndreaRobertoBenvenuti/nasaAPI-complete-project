package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.NeoAsteroid;
import it.polimi.nasa.nasabackend.entity.NeoCloseApproach;
import it.polimi.nasa.nasabackend.repository.NeoAsteroidRepository;
import it.polimi.nasa.nasabackend.repository.NeoCloseApproachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class NeoService {

    @Autowired
    private NeoAsteroidRepository neoAsteroidRepository;

    @Autowired
    private NeoCloseApproachRepository neoCloseApproachRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String apiKey;

    private static final String NEO_FEED_URL = "https://api.nasa.gov/neo/rest/v1/feed";

    // Formatter per parsing date API (Locale.US fondamentale per "Sep", "Oct", ecc)
    private static final DateTimeFormatter APPROACH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm", Locale.US);

    @Transactional
    public Map<String, Integer> fetchAndSaveNeo(String startDateStr, String endDateStr) {
        System.out.println("🪐 Fetching NEO (Asteroid) data from NASA...");
        System.out.println("   📅 Requested Range: " + startDateStr + " to " + endDateStr);

        // 1. Setup API Source Metadata
        ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                "NASA_NeoWs",
                NEO_FEED_URL,
                "NASA Near Earth Object Web Service"
        );

        // 2. Calcolo dei blocchi di 7 giorni
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = LocalDate.parse(endDateStr);
        LocalDate currentStart = start;

        int totalAsteroids = 0;
        int totalApproaches = 0;

        // 3. Ciclo sui blocchi temporali
        while (currentStart.isBefore(end) || currentStart.equals(end)) {
            LocalDate currentEnd = currentStart.plusDays(7);

            // Non superare la data finale richiesta
            if (currentEnd.isAfter(end)) {
                currentEnd = end;
            }

            System.out.println("   🔄 Processing batch: " + currentStart + " -> " + currentEnd);

            try {
                // Esegui il fetch per il blocco corrente
                Map<String, Integer> batchResult = fetchBatch(currentStart.toString(), currentEnd.toString(), apiSource);
                totalAsteroids += batchResult.get("asteroids");
                totalApproaches += batchResult.get("approaches");

                // Piccolo delay per evitare Rate Limiting (429) della NASA
                Thread.sleep(500);

            } catch (Exception e) {
                System.err.println("   ⚠️ Error in batch " + currentStart + ": " + e.getMessage());
            }

            // Passa al prossimo blocco
            currentStart = currentEnd.plusDays(1);
        }

        apiSourceService.updateApiSourceStats("NASA_NeoWs", totalAsteroids);
        System.out.println("   ✅ Processed Total: " + totalAsteroids + " asteroids, " + totalApproaches + " approaches");

        return Map.of("asteroids", totalAsteroids, "approaches", totalApproaches);
    }

    /**
     * Esegue la chiamata API per un singolo blocco di max 7 giorni
     */
    private Map<String, Integer> fetchBatch(String start, String end, ApiSource apiSource) {
        int newAsteroids = 0;
        int newApproaches = 0;

        try {
            String url = String.format("%s?start_date=%s&end_date=%s&api_key=%s",
                    NEO_FEED_URL, start, end, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.contains("\"error\"")) {
                return Map.of("asteroids", 0, "approaches", 0);
            }

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

            if (!jsonObject.has("near_earth_objects")) {
                return Map.of("asteroids", 0, "approaches", 0);
            }

            JsonObject nearEarthObjects = jsonObject.getAsJsonObject("near_earth_objects");

            for (String date : nearEarthObjects.keySet()) {
                JsonArray asteroids = nearEarthObjects.getAsJsonArray(date);

                for (JsonElement element : asteroids) {
                    try {
                        JsonObject asteroidJson = element.getAsJsonObject();
                        String neoRefId = asteroidJson.get("neo_reference_id").getAsString();

                        // Gestione Asteroide
                        NeoAsteroid asteroid;
                        Optional<NeoAsteroid> existing = neoAsteroidRepository.findByNeoReferenceId(neoRefId);

                        if (existing.isPresent()) {
                            asteroid = existing.get();
                        } else {
                            asteroid = parseAsteroidData(asteroidJson, apiSource);
                            asteroid = neoAsteroidRepository.save(asteroid);
                            newAsteroids++;
                        }

                        // Gestione Close Approaches
                        if (asteroidJson.has("close_approach_data")) {
                            JsonArray closeApproaches = asteroidJson.getAsJsonArray("close_approach_data");
                            for (JsonElement approachElement : closeApproaches) {
                                JsonObject approachJson = approachElement.getAsJsonObject();
                                NeoCloseApproach approach = parseCloseApproach(approachJson, asteroid);

                                // Qui potresti aggiungere un check exists se necessario
                                neoCloseApproachRepository.save(approach);
                                newApproaches++;
                            }
                        }

                    } catch (Exception e) {
                        // Skip single entry error
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("   ❌ Batch Error: " + e.getMessage());
        }

        return Map.of("asteroids", newAsteroids, "approaches", newApproaches);
    }

    private NeoAsteroid parseAsteroidData(JsonObject json, ApiSource apiSource) {
        NeoAsteroid asteroid = new NeoAsteroid();

        asteroid.setNeoReferenceId(json.get("neo_reference_id").getAsString());
        asteroid.setName(json.get("name").getAsString());
        asteroid.setIsPotentiallyHazardous(json.get("is_potentially_hazardous_asteroid").getAsBoolean());

        if (json.has("is_sentry_object") && !json.get("is_sentry_object").isJsonNull()) {
            asteroid.setIsSentryObject(json.get("is_sentry_object").getAsBoolean());
        }

        if (json.has("absolute_magnitude_h") && !json.get("absolute_magnitude_h").isJsonNull()) {
            asteroid.setAbsoluteMagnitudeH(new BigDecimal(json.get("absolute_magnitude_h").getAsString()));
        }

        if (json.has("estimated_diameter")) {
            JsonObject diameter = json.getAsJsonObject("estimated_diameter");
            if (diameter.has("kilometers")) {
                JsonObject km = diameter.getAsJsonObject("kilometers");
                asteroid.setEstimatedDiameterKmMin(new BigDecimal(km.get("estimated_diameter_min").getAsString()));
                asteroid.setEstimatedDiameterKmMax(new BigDecimal(km.get("estimated_diameter_max").getAsString()));
            }
            if (diameter.has("meters")) {
                JsonObject m = diameter.getAsJsonObject("meters");
                asteroid.setEstimatedDiameterMMin(new BigDecimal(m.get("estimated_diameter_min").getAsString()));
                asteroid.setEstimatedDiameterMMax(new BigDecimal(m.get("estimated_diameter_max").getAsString()));
            }
        }

        // Orbital data (spesso assente nel FEED, presente nel LOOKUP)
        if (json.has("orbital_data") && !json.get("orbital_data").isJsonNull()) {
            JsonObject orbital = json.getAsJsonObject("orbital_data");
            if (orbital.has("orbital_period") && !orbital.get("orbital_period").isJsonNull())
                asteroid.setOrbitalPeriodDays(new BigDecimal(orbital.get("orbital_period").getAsString()));
            if (orbital.has("eccentricity") && !orbital.get("eccentricity").isJsonNull())
                asteroid.setOrbitEccentricity(new BigDecimal(orbital.get("eccentricity").getAsString()));
            if (orbital.has("semi_major_axis") && !orbital.get("semi_major_axis").isJsonNull())
                asteroid.setSemiMajorAxisAu(new BigDecimal(orbital.get("semi_major_axis").getAsString()));
            if (orbital.has("inclination") && !orbital.get("inclination").isJsonNull())
                asteroid.setInclinationDeg(new BigDecimal(orbital.get("inclination").getAsString()));
        }

        if (json.has("nasa_jpl_url")) {
            asteroid.setNasaJplUrl(json.get("nasa_jpl_url").getAsString());
        }

        asteroid.setApiSource(apiSource);
        asteroid.setRawData(json.toString());

        return asteroid;
    }

    private NeoCloseApproach parseCloseApproach(JsonObject json, NeoAsteroid asteroid) {
        NeoCloseApproach approach = new NeoCloseApproach();

        approach.setNeo(asteroid);

        // Date parsing robusto con Locale.US
        if (json.has("close_approach_date_full") && !json.get("close_approach_date_full").isJsonNull()) {
            approach.setApproachDate(parseDateTime(json.get("close_approach_date_full").getAsString()));
        } else if (json.has("close_approach_date") && !json.get("close_approach_date").isJsonNull()) {
            String simpleDate = json.get("close_approach_date").getAsString() + " 00:00";
            try {
                approach.setApproachDate(LocalDateTime.parse(simpleDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } catch (Exception e) { /* Ignora */ }
        }

        if (json.has("miss_distance")) {
            JsonObject missDistance = json.getAsJsonObject("miss_distance");
            if (missDistance.has("kilometers"))
                approach.setMissDistanceKm(new BigDecimal(missDistance.get("kilometers").getAsString()));
            if (missDistance.has("astronomical"))
                approach.setMissDistanceAu(new BigDecimal(missDistance.get("astronomical").getAsString()));
            if (missDistance.has("lunar"))
                approach.setMissDistanceLunar(new BigDecimal(missDistance.get("lunar").getAsString()));
        }

        if (json.has("relative_velocity")) {
            JsonObject velocity = json.getAsJsonObject("relative_velocity");
            if (velocity.has("kilometers_per_second"))
                approach.setRelativeVelocityKmS(new BigDecimal(velocity.get("kilometers_per_second").getAsString()));
            if (velocity.has("kilometers_per_hour"))
                approach.setRelativeVelocityKmH(new BigDecimal(velocity.get("kilometers_per_hour").getAsString()));
        }

        if (json.has("orbiting_body")) {
            approach.setOrbitingBody(json.get("orbiting_body").getAsString());
        }

        return approach;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, APPROACH_DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    public long getAsteroidCount() { return neoAsteroidRepository.count(); }
    public long getApproachCount() { return neoCloseApproachRepository.count(); }
}