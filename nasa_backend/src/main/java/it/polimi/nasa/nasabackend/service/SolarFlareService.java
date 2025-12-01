package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.SolarFlare;
import it.polimi.nasa.nasabackend.repository.SolarFlareRepository;
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
public class SolarFlareService {

    @Autowired
    private SolarFlareRepository solarFlareRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String apiKey;

    private static final String DONKI_FLARE_URL = "https://api.nasa.gov/DONKI/FLR";

    // Formatter universale per NASA DONKI (gestisce date con o senza secondi)
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    public List<SolarFlare> fetchAndSaveFlares(String startDate, String endDate) {
        System.out.println("☀️ Fetching Solar Flare data from NASA DONKI...");

        try {
            ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                    "NASA_DONKI_Flare",
                    DONKI_FLARE_URL,
                    "NASA DONKI Solar Flare Events"
            );

            String url = String.format("%s?startDate=%s&endDate=%s&api_key=%s",
                    DONKI_FLARE_URL, startDate, endDate, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                System.out.println("ℹ️ No solar flares found in date range");
                return new ArrayList<>();
            }

            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            List<SolarFlare> flares = new ArrayList<>();
            int newRecords = 0;

            for (JsonElement element : jsonArray) {
                JsonObject flareJson = element.getAsJsonObject();

                try {
                    String activityId = flareJson.get("flrID").getAsString();

                    // Skip if already exists
                    if (solarFlareRepository.existsByActivityId(activityId)) {
                        continue;
                    }

                    SolarFlare flare = parseFlareData(flareJson, apiSource);

                    // Controllo critico: se peakTime manca, logga e salta
                    if (flare.getPeakTime() == null) {
                        System.err.println("⚠️ Skipping Flare " + activityId + " due to missing peakTime");
                        continue;
                    }

                    SolarFlare saved = solarFlareRepository.save(flare);
                    flares.add(saved);
                    newRecords++;

                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing flare: " + e.getMessage());
                }
            }

            apiSourceService.updateApiSourceStats("NASA_DONKI_Flare", newRecords);
            System.out.println("✅ Saved " + newRecords + " solar flare records");

            return flares;

        } catch (Exception e) {
            System.err.println("❌ Error fetching solar flare data: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private SolarFlare parseFlareData(JsonObject json, ApiSource apiSource) {
        SolarFlare flare = new SolarFlare();

        // Activity ID
        if (json.has("flrID")) {
            flare.setActivityId(json.get("flrID").getAsString());
        }

        // Timing (Usa parseIsoDateTime robusto)
        if (json.has("beginTime") && !json.get("beginTime").isJsonNull()) {
            flare.setBeginTime(parseIsoDateTime(json.get("beginTime").getAsString()));
        }

        if (json.has("peakTime") && !json.get("peakTime").isJsonNull()) {
            flare.setPeakTime(parseIsoDateTime(json.get("peakTime").getAsString()));
        }

        if (json.has("endTime") && !json.get("endTime").isJsonNull()) {
            flare.setEndTime(parseIsoDateTime(json.get("endTime").getAsString()));
        }

        // Classification (es. "X2.5")
        if (json.has("classType") && !json.get("classType").isJsonNull()) {
            String classType = json.get("classType").getAsString();
            flare.setFullClass(classType);

            // Parsing sicuro di Tipo e Intensità
            if (classType != null && classType.length() >= 1) {
                // Primo carattere è la classe (A, B, C, M, X)
                flare.setClassType(classType.substring(0, 1));

                // Il resto è l'intensità numerica
                if (classType.length() > 1) {
                    try {
                        String intensityStr = classType.substring(1);
                        flare.setClassIntensity(new BigDecimal(intensityStr));
                    } catch (NumberFormatException e) {
                        flare.setClassIntensity(BigDecimal.ZERO);
                    }
                } else {
                    flare.setClassIntensity(BigDecimal.ZERO);
                }
            }
        }

        // Source location
        if (json.has("sourceLocation") && !json.get("sourceLocation").isJsonNull()) {
            flare.setSourceLocation(json.get("sourceLocation").getAsString());
        }

        // Active region (Gestione sicura Integer)
        if (json.has("activeRegionNum") && !json.get("activeRegionNum").isJsonNull()) {
            try {
                flare.setActiveRegionNum(json.get("activeRegionNum").getAsInt());
            } catch (NumberFormatException e) { /* Ignore non-numeric region IDs */ }
        }

        // Instruments (Loop sicuro con check JsonObject)
        if (json.has("instruments") && !json.get("instruments").isJsonNull()) {
            JsonArray instruments = json.getAsJsonArray("instruments");
            List<String> instrumentList = new ArrayList<>();
            for (JsonElement inst : instruments) {
                if (inst.isJsonObject() && inst.getAsJsonObject().has("displayName")) {
                    instrumentList.add(inst.getAsJsonObject().get("displayName").getAsString());
                }
            }
            flare.setInstruments(String.join(", ", instrumentList));
        }

        // Metadata
        if (json.has("note") && !json.get("note").isJsonNull()) {
            flare.setNote(json.get("note").getAsString());
        }

        if (json.has("linkedEvents") && !json.get("linkedEvents").isJsonNull()) {
            flare.setLinkedEvents(json.get("linkedEvents").toString());
        }

        if (json.has("submissionTime") && !json.get("submissionTime").isJsonNull()) {
            flare.setSubmissionTime(parseIsoDateTime(json.get("submissionTime").getAsString()));
        }

        if (json.has("versionId") && !json.get("versionId").isJsonNull()) {
            flare.setVersionId(json.get("versionId").getAsInt());
        }

        flare.setApiSource(apiSource);
        flare.setRawData(json.toString());

        return flare;
    }

    private LocalDateTime parseIsoDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            // Rimuove 'Z' se presente
            String cleanDate = isoDate.endsWith("Z") ? isoDate.substring(0, isoDate.length() - 1) : isoDate;
            return LocalDateTime.parse(cleanDate, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            System.err.println("⚠️ Date parsing warning for: " + isoDate);
            return null; // Ritorna null, NON now()
        }
    }

    public long getFlareCount() {
        return solarFlareRepository.count();
    }

    public List<SolarFlare> getAllFlares() {
        return solarFlareRepository.findAll();
    }
}