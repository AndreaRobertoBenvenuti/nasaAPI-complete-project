package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.CoronalMassEjection;
import it.polimi.nasa.nasabackend.repository.CoronalMassEjectionRepository;
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
public class CoronalMassEjectionService {

    @Autowired
    private CoronalMassEjectionRepository cmeRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String apiKey;

    private static final String DONKI_CME_URL = "https://api.nasa.gov/DONKI/CME";

    // Formatter flessibile che accetta date con o senza secondi
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    public List<CoronalMassEjection> fetchAndSaveCme(String startDate, String endDate) {
        System.out.println("🌊 Fetching CME data from NASA DONKI...");

        try {
            ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                    "NASA_DONKI_CME",
                    DONKI_CME_URL,
                    "NASA DONKI Coronal Mass Ejection Events"
            );

            // URL corretto
            String url = String.format("%s?startDate=%s&endDate=%s&api_key=%s",
                    DONKI_CME_URL, startDate, endDate, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                System.out.println("ℹ️ No CME events found in date range");
                return new ArrayList<>();
            }

            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            List<CoronalMassEjection> cmeList = new ArrayList<>();
            int newRecords = 0;

            for (JsonElement element : jsonArray) {
                JsonObject cmeJson = element.getAsJsonObject();

                try {
                    String activityId = cmeJson.get("activityID").getAsString();

                    // Skip if already exists
                    if (cmeRepository.existsByActivityId(activityId)) {
                        continue;
                    }

                    CoronalMassEjection cme = parseCmeData(cmeJson, apiSource);
                    CoronalMassEjection saved = cmeRepository.save(cme);
                    cmeList.add(saved);
                    newRecords++;

                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing CME: " + e.getMessage());
                }
            }

            apiSourceService.updateApiSourceStats("NASA_DONKI_CME", newRecords);
            System.out.println("✅ Saved " + newRecords + " CME records");

            return cmeList;

        } catch (Exception e) {
            System.err.println("❌ Error fetching CME data: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private CoronalMassEjection parseCmeData(JsonObject json, ApiSource apiSource) {
        CoronalMassEjection cme = new CoronalMassEjection();

        // Activity ID
        cme.setActivityId(json.get("activityID").getAsString());

        // Start time
        if (json.has("startTime") && !json.get("startTime").isJsonNull()) {
            cme.setStartTime(parseIsoDateTime(json.get("startTime").getAsString()));
        }

        // Source location
        if (json.has("sourceLocation") && !json.get("sourceLocation").isJsonNull()) {
            cme.setSourceLocation(json.get("sourceLocation").getAsString());
        }

        // Note
        if (json.has("note") && !json.get("note").isJsonNull()) {
            cme.setNote(json.get("note").getAsString());
        }

        // Instruments
        if (json.has("instruments") && !json.get("instruments").isJsonNull()) {
            JsonArray instruments = json.getAsJsonArray("instruments");
            List<String> instrumentList = new ArrayList<>();
            for (JsonElement inst : instruments) {
                // Controllo extra per evitare NullPointerException interna
                if (inst.isJsonObject() && inst.getAsJsonObject().has("displayName")) {
                    instrumentList.add(inst.getAsJsonObject().get("displayName").getAsString());
                }
            }
            cme.setInstruments(String.join(", ", instrumentList));
        }

        // ============================================
        // NUOVI CAMPI - Metadata
        // ============================================
        if (json.has("submissionTime") && !json.get("submissionTime").isJsonNull()) {
            cme.setSubmissionTime(parseIsoDateTime(json.get("submissionTime").getAsString()));
        }

        if (json.has("versionId") && !json.get("versionId").isJsonNull()) {
            cme.setVersionId(json.get("versionId").getAsInt());
        }

        // Linked Events (salva JSON array come string)
        if (json.has("linkedEvents") && !json.get("linkedEvents").isJsonNull()) {
            cme.setLinkedEvents(json.get("linkedEvents").toString());
        }

        // ============================================

        // CME Analysis
        if (json.has("cmeAnalyses") && !json.get("cmeAnalyses").isJsonNull()) {
            JsonArray analyses = json.getAsJsonArray("cmeAnalyses");
            if (analyses.size() > 0) {
                // Cerca l'analisi più accurata
                JsonObject analysis = null;
                for (JsonElement analysisElement : analyses) {
                    JsonObject current = analysisElement.getAsJsonObject();
                    if (current.has("isMostAccurate") && current.get("isMostAccurate").getAsBoolean()) {
                        analysis = current;
                        cme.setIsMostAccurate(true);
                        break;
                    }
                }

                // Se non trovata, usa la prima
                if (analysis == null) {
                    analysis = analyses.get(0).getAsJsonObject();
                    // Importante: setta esplicitamente il flag se presente nel primo elemento
                    if (analysis.has("isMostAccurate")) {
                        cme.setIsMostAccurate(analysis.get("isMostAccurate").getAsBoolean());
                    }
                }

                // Parse campi analisi
                if (analysis.has("speed") && !analysis.get("speed").isJsonNull()) {
                    try {
                        cme.setSpeedKmS(new BigDecimal(analysis.get("speed").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                if (analysis.has("halfAngle") && !analysis.get("halfAngle").isJsonNull()) {
                    try {
                        cme.setHalfAngleDeg(new BigDecimal(analysis.get("halfAngle").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                if (analysis.has("type") && !analysis.get("type").isJsonNull()) {
                    cme.setType(analysis.get("type").getAsString());
                }

                if (analysis.has("latitude") && !analysis.get("latitude").isJsonNull()) {
                    try {
                        cme.setLatitude(new BigDecimal(analysis.get("latitude").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                if (analysis.has("longitude") && !analysis.get("longitude").isJsonNull()) {
                    try {
                        cme.setLongitude(new BigDecimal(analysis.get("longitude").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                // Nuovi campi Analysis
                if (analysis.has("time21_5") && !analysis.get("time21_5").isJsonNull()) {
                    cme.setTime215(parseIsoDateTime(analysis.get("time21_5").getAsString()));
                }

                if (analysis.has("featureCode") && !analysis.get("featureCode").isJsonNull()) {
                    cme.setFeatureCode(analysis.get("featureCode").getAsString());
                }

                if (analysis.has("imageType") && !analysis.get("imageType").isJsonNull()) {
                    cme.setImageType(analysis.get("imageType").getAsString());
                }

                if (analysis.has("measurementTechnique") && !analysis.get("measurementTechnique").isJsonNull()) {
                    cme.setMeasurementTechnique(analysis.get("measurementTechnique").getAsString());
                }

                if (analysis.has("levelOfData") && !analysis.get("levelOfData").isJsonNull()) {
                    cme.setLevelOfData(analysis.get("levelOfData").getAsInt());
                }

                if (analysis.has("tilt") && !analysis.get("tilt").isJsonNull()) {
                    try {
                        cme.setTilt(new BigDecimal(analysis.get("tilt").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                if (analysis.has("minorHalfWidth") && !analysis.get("minorHalfWidth").isJsonNull()) {
                    try {
                        cme.setMinorHalfWidth(new BigDecimal(analysis.get("minorHalfWidth").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }

                if (analysis.has("speedMeasuredAtHeight") && !analysis.get("speedMeasuredAtHeight").isJsonNull()) {
                    try {
                        cme.setSpeedMeasuredAtHeight(new BigDecimal(analysis.get("speedMeasuredAtHeight").getAsString()));
                    } catch (NumberFormatException e) { /* Ignore */ }
                }
            }
        }

        cme.setApiSource(apiSource);
        cme.setRawData(json.toString());

        return cme;
    }

    private LocalDateTime parseIsoDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            // Rimuove 'Z' finale se presente per parsing locale
            String cleanDate = isoDate.endsWith("Z") ? isoDate.substring(0, isoDate.length() - 1) : isoDate;
            return LocalDateTime.parse(cleanDate, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            System.err.println("⚠️ Date parsing error for: " + isoDate);
            return null; // Meglio null che una data sbagliata (now)
        }
    }

    public long getCmeCount() {
        return cmeRepository.count();
    }

    public List<CoronalMassEjection> getAllCme() {
        return cmeRepository.findAll();
    }
}