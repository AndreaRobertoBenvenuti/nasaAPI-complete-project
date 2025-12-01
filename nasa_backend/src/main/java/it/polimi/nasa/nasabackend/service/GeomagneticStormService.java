package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.GeomagneticStorm;
import it.polimi.nasa.nasabackend.repository.GeomagneticStormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeomagneticStormService {

    @Autowired
    private GeomagneticStormRepository geomagneticStormRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String apiKey;

    private static final String DONKI_GST_URL = "https://api.nasa.gov/DONKI/GST";

    // Formatter robusto (gestisce date con e senza secondi)
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    public List<GeomagneticStorm> fetchAndSaveStorms(String startDate, String endDate) {
        System.out.println("🌍 Fetching Geomagnetic Storm data from NASA DONKI...");

        try {
            ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                    "NASA_DONKI_GST",
                    DONKI_GST_URL,
                    "NASA DONKI Geomagnetic Storm Events"
            );

            String url = String.format("%s?startDate=%s&endDate=%s&api_key=%s",
                    DONKI_GST_URL, startDate, endDate, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                System.out.println("ℹ️ No geomagnetic storms found in date range");
                return new ArrayList<>();
            }

            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            List<GeomagneticStorm> storms = new ArrayList<>();
            int newRecords = 0;

            for (JsonElement element : jsonArray) {
                JsonObject stormJson = element.getAsJsonObject();

                try {
                    String activityId = stormJson.get("gstID").getAsString();

                    // Skip if already exists
                    if (geomagneticStormRepository.existsByActivityId(activityId)) {
                        continue;
                    }

                    GeomagneticStorm storm = parseStormData(stormJson, apiSource);
                    GeomagneticStorm saved = geomagneticStormRepository.save(storm);
                    storms.add(saved);
                    newRecords++;

                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing storm: " + e.getMessage());
                }
            }

            apiSourceService.updateApiSourceStats("NASA_DONKI_GST", newRecords);
            System.out.println("✅ Saved " + newRecords + " geomagnetic storm records");

            return storms;

        } catch (Exception e) {
            System.err.println("❌ Error fetching geomagnetic storm data: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private GeomagneticStorm parseStormData(JsonObject json, ApiSource apiSource) {
        GeomagneticStorm storm = new GeomagneticStorm();

        // Activity ID
        storm.setActivityId(json.get("gstID").getAsString());

        // Start time
        if (json.has("startTime") && !json.get("startTime").isJsonNull()) {
            storm.setStartTime(parseIsoDateTime(json.get("startTime").getAsString()));
        }

        // ============================================
        // LOGICA AVANZATA: KP, G-SCALE e END TIME
        // ============================================
        if (json.has("allKpIndex") && !json.get("allKpIndex").isJsonNull()) {
            JsonArray kpArray = json.getAsJsonArray("allKpIndex");

            // 1. Salva il JSON grezzo
            storm.setAllKpIndex(kpArray.toString());

            double maxKp = 0.0;
            LocalDateTime lastObservedTime = null;

            // 2. Itera per trovare Max Kp e l'ultimo orario (EndTime)
            for (JsonElement kp : kpArray) {
                JsonObject kpObj = kp.getAsJsonObject();

                // Cerca max KP
                if (kpObj.has("kpIndex") && !kpObj.get("kpIndex").isJsonNull()) {
                    double currentKp = kpObj.get("kpIndex").getAsDouble();
                    if (currentKp > maxKp) {
                        maxKp = currentKp;
                    }
                }

                // Cerca ultima data utile
                if (kpObj.has("observedTime") && !kpObj.get("observedTime").isJsonNull()) {
                    LocalDateTime obsTime = parseIsoDateTime(kpObj.get("observedTime").getAsString());
                    if (obsTime != null) {
                        if (lastObservedTime == null || obsTime.isAfter(lastObservedTime)) {
                            lastObservedTime = obsTime;
                        }
                    }
                }
            }

            // Setta i valori calcolati
            if (maxKp > 0) {
                storm.setKpIndex(BigDecimal.valueOf(maxKp));
                storm.setGScale(calculateGScale(maxKp)); // Calcola G-Scale automaticamente
            }

            if (lastObservedTime != null) {
                storm.setEndTime(lastObservedTime);
            }
        }

        // ============================================
        // METADATA
        // ============================================
        if (json.has("linkedEvents") && !json.get("linkedEvents").isJsonNull()) {
            storm.setLinkedEvents(json.get("linkedEvents").toString());
        }

        if (json.has("submissionTime") && !json.get("submissionTime").isJsonNull()) {
            storm.setSubmissionTime(parseIsoDateTime(json.get("submissionTime").getAsString()));
        }

        if (json.has("versionId") && !json.get("versionId").isJsonNull()) {
            storm.setVersionId(json.get("versionId").getAsInt());
        }

        // Instruments
        if (json.has("instruments") && !json.get("instruments").isJsonNull()) {
            JsonArray instruments = json.getAsJsonArray("instruments");
            List<String> instrumentList = new ArrayList<>();
            for (JsonElement inst : instruments) {
                if (inst.isJsonObject() && inst.getAsJsonObject().has("displayName")) {
                    instrumentList.add(inst.getAsJsonObject().get("displayName").getAsString());
                }
            }
            storm.setInstruments(String.join(", ", instrumentList));
        }

        storm.setApiSource(apiSource);
        storm.setRawData(json.toString());

        return storm;
    }

    /**
     * Calcola la scala NOAA G (G1-G5) basata sull'indice Kp.
     * Kp >= 5 -> G1, Kp >= 6 -> G2, ecc.
     */
    private Integer calculateGScale(double kp) {
        if (kp >= 9) return 5;
        if (kp >= 8) return 4;
        if (kp >= 7) return 3;
        if (kp >= 6) return 2;
        if (kp >= 5) return 1;
        return 0; // G0 (Nessuna tempesta significativa)
    }

    private LocalDateTime parseIsoDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            String cleanDate = isoDate.endsWith("Z") ? isoDate.substring(0, isoDate.length() - 1) : isoDate;
            return LocalDateTime.parse(cleanDate, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            // Loggare l'errore senza spaccare il loop principale
            System.err.println("⚠️ Date parsing warning: " + isoDate);
            return null;
        }
    }

    public long getStormCount() {
        return geomagneticStormRepository.count();
    }

    public List<GeomagneticStorm> getAllStorms() {
        return geomagneticStormRepository.findAll();
    }
}