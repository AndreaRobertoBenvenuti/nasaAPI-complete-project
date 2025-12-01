package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.InterplanetaryShock;
import it.polimi.nasa.nasabackend.repository.InterplanetaryShockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterplanetaryShockService {

    @Autowired
    private InterplanetaryShockRepository ipsRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nasa.api.key}")
    private String apiKey;

    private static final String DONKI_IPS_URL = "https://api.nasa.gov/DONKI/IPS";

    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    public List<InterplanetaryShock> fetchAndSaveIps(String startDate, String endDate) {
        System.out.println("🌊 Fetching Interplanetary Shock data from NASA DONKI...");

        try {
            ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                    "NASA_DONKI_IPS",
                    DONKI_IPS_URL,
                    "NASA DONKI Interplanetary Shock Events"
            );

            String url = String.format("%s?startDate=%s&endDate=%s&api_key=%s",
                    DONKI_IPS_URL, startDate, endDate, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                System.out.println("ℹ️ No IPS events found in date range");
                return new ArrayList<>();
            }

            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
            List<InterplanetaryShock> ipsList = new ArrayList<>();
            int newRecords = 0;

            for (JsonElement element : jsonArray) {
                JsonObject ipsJson = element.getAsJsonObject();

                try {
                    String activityId = ipsJson.get("activityID").getAsString();

                    if (ipsRepository.existsByActivityId(activityId)) {
                        continue;
                    }

                    InterplanetaryShock ips = parseIpsData(ipsJson, apiSource);

                    if (ips.getActivityTime() == null) {
                        System.err.println("⚠️ Skipping IPS " + activityId + " due to missing eventTime");
                        continue;
                    }

                    InterplanetaryShock saved = ipsRepository.save(ips);
                    ipsList.add(saved);
                    newRecords++;

                } catch (Exception e) {
                    System.err.println("⚠️ Error parsing IPS: " + e.getMessage());
                }
            }

            apiSourceService.updateApiSourceStats("NASA_DONKI_IPS", newRecords);
            System.out.println("✅ Saved " + newRecords + " IPS records");

            return ipsList;

        } catch (Exception e) {
            System.err.println("❌ Error fetching IPS data: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private InterplanetaryShock parseIpsData(JsonObject json, ApiSource apiSource) {
        InterplanetaryShock ips = new InterplanetaryShock();

        if (json.has("activityID") && !json.get("activityID").isJsonNull()) {
            ips.setActivityId(json.get("activityID").getAsString());
        }

        if (json.has("catalog") && !json.get("catalog").isJsonNull()) {
            ips.setCatalog(json.get("catalog").getAsString());
        }

        if (json.has("eventTime") && !json.get("eventTime").isJsonNull()) {
            ips.setActivityTime(parseIsoDateTime(json.get("eventTime").getAsString()));
        }

        if (json.has("location") && !json.get("location").isJsonNull()) {
            ips.setLocation(json.get("location").getAsString());
        }

        if (json.has("linkedEvents") && !json.get("linkedEvents").isJsonNull()) {
            ips.setLinkedEvents(json.get("linkedEvents").toString());
        }

        if (json.has("submissionTime") && !json.get("submissionTime").isJsonNull()) {
            ips.setSubmissionTime(parseIsoDateTime(json.get("submissionTime").getAsString()));
        }

        if (json.has("versionId") && !json.get("versionId").isJsonNull()) {
            ips.setVersionId(json.get("versionId").getAsInt());
        }

        if (json.has("instruments") && !json.get("instruments").isJsonNull()) {
            JsonArray instruments = json.getAsJsonArray("instruments");
            List<String> instrumentList = new ArrayList<>();
            for (JsonElement inst : instruments) {
                if (inst.isJsonObject() && inst.getAsJsonObject().has("displayName")) {
                    instrumentList.add(inst.getAsJsonObject().get("displayName").getAsString());
                }
            }
            ips.setInstruments(String.join(", ", instrumentList));
        }

        ips.setApiSource(apiSource);
        ips.setRawData(json.toString());

        return ips;
    }

    private LocalDateTime parseIsoDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            String cleanDate = isoDate.endsWith("Z") ? isoDate.substring(0, isoDate.length() - 1) : isoDate;
            return LocalDateTime.parse(cleanDate, FLEXIBLE_FORMATTER);
        } catch (Exception e) {
            System.err.println("⚠️ Date parsing warning for: " + isoDate);
            return null;
        }
    }

    public long getIpsCount() {
        return ipsRepository.count();
    }

    public List<InterplanetaryShock> getAllIps() {
        return ipsRepository.findAllByOrderByActivityTimeDesc();
    }

    /**
     * NUOVO METODO: Recupera solo gli shock diretti verso la Terra
     */
    public List<InterplanetaryShock> getEarthShocks() {
        return ipsRepository.findEarthShocks();
    }
}