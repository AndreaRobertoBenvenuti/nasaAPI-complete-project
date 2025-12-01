package it.polimi.nasa.nasabackend.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.entity.Fireball;
import it.polimi.nasa.nasabackend.repository.FireballRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class FireballService {

    @Autowired
    private FireballRepository fireballRepository;

    @Autowired
    private ApiSourceService apiSourceService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String FIREBALL_URL = "https://ssd-api.jpl.nasa.gov/fireball.api";

    public Map<String, Integer> fetchAndSaveFireballs() {
        System.out.println("🔥 Fetching Fireball data from NASA...");

        try {
            ApiSource apiSource = apiSourceService.getOrCreateApiSource(
                    "NASA_Fireball",
                    FIREBALL_URL,
                    "NASA Fireball and Bolide Data"
            );

            String response = restTemplate.getForObject(FIREBALL_URL, String.class);

            if (response == null) {
                System.err.println("   ❌ No response from Fireball API");
                return Map.of("fireballs", 0);
            }

            JsonElement jsonElement = JsonParser.parseString(response);
            JsonArray dataArray = jsonElement.getAsJsonObject().getAsJsonArray("data");

            int newFireballs = 0;
            int skippedFireballs = 0;

            for (JsonElement element : dataArray) {
                try {
                    JsonArray fireballData = element.getAsJsonArray();
                    Fireball fireball = parseFireballData(fireballData, apiSource);

                    if (fireball != null) {
                        fireballRepository.save(fireball);
                        newFireballs++;
                    } else {
                        skippedFireballs++;
                    }
                } catch (Exception e) {
                    skippedFireballs++;
                    // Silently skip malformed entries
                }
            }

            apiSourceService.updateApiSourceStats("NASA_Fireball", newFireballs);
            System.out.println("   ✅ Saved " + newFireballs + " fireballs");

            if (skippedFireballs > 0) {
                System.out.println("   ⚠️ Skipped " + skippedFireballs + " entries with incomplete data");
            }

            return Map.of("fireballs", newFireballs);

        } catch (Exception e) {
            System.err.println("   ❌ Error fetching Fireball data: " + e.getMessage());
            return Map.of("fireballs", 0);
        }
    }

    private Fireball parseFireballData(JsonArray data, ApiSource apiSource) {
        try {
            // Verifica che l'array abbia almeno gli elementi base fino alla longitudine (indice 6)
            // Struttura attesa: [date, energy, impact, lat, lat-dir, lon, lon-dir, alt, vel, vx, vy, vz]
            if (data.size() < 7) {
                return null; // Skip entries with insufficient data
            }

            Fireball fireball = new Fireball();

            // 0. Date: "2025-11-20 12:30:00"
            if (!data.get(0).isJsonNull()) {
                String dateStr = data.get(0).getAsString();
                fireball.setEventDate(parseDateTime(dateStr));
            }

            // 1. Total Radiated Energy
            if (!data.get(1).isJsonNull()) {
                try {
                    fireball.setTotalRadiatedEnergyJ(new BigDecimal(data.get(1).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // 2. Total Impact Energy
            if (!data.get(2).isJsonNull()) {
                try {
                    fireball.setTotalImpactEnergyKt(new BigDecimal(data.get(2).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // 3 & 4. Latitude (Valore + Direzione)
            if (!data.get(3).isJsonNull() && !data.get(4).isJsonNull()) {
                String latVal = data.get(3).getAsString();
                String latDir = data.get(4).getAsString();
                fireball.setLatitude(calculateSignedCoordinate(latVal, latDir));
            }

            // 5 & 6. Longitude (Valore + Direzione)
            if (!data.get(5).isJsonNull() && !data.get(6).isJsonNull()) {
                String lonVal = data.get(5).getAsString();
                String lonDir = data.get(6).getAsString();
                fireball.setLongitude(calculateSignedCoordinate(lonVal, lonDir));
            }

            // 7. Altitude (Indice cambiato da 5 a 7)
            if (data.size() > 7 && !data.get(7).isJsonNull()) {
                try {
                    fireball.setAltitudeKm(new BigDecimal(data.get(7).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // 8. Velocity (Indice cambiato da 6 a 8)
            if (data.size() > 8 && !data.get(8).isJsonNull()) {
                try {
                    fireball.setVelocityKmS(new BigDecimal(data.get(8).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // 9, 10, 11. Velocity components vx, vy, vz (Opzionali)
            // Controlliamo se esistono nell'array, dato che non sempre vengono inviati

            // vx
            if (data.size() > 9 && !data.get(9).isJsonNull()) {
                try {
                    fireball.setVelocityVx(new BigDecimal(data.get(9).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // vy
            if (data.size() > 10 && !data.get(10).isJsonNull()) {
                try {
                    fireball.setVelocityVy(new BigDecimal(data.get(10).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            // vz
            if (data.size() > 11 && !data.get(11).isJsonNull()) {
                try {
                    fireball.setVelocityVz(new BigDecimal(data.get(11).getAsString()));
                } catch (NumberFormatException e) { /* Ignora */ }
            }

            fireball.setApiSource(apiSource);
            fireball.setRawData(data.toString());

            return fireball;

        } catch (Exception e) {
            // Ritorna null per skippare questa entry specifica senza bloccare tutto il processo
            return null;
        }
    }

    /**
     * Calcola la coordinata corretta combinando valore e direzione.
     * Es: Valore "10.5", Direzione "S" -> Ritorna -10.5
     */
    private BigDecimal calculateSignedCoordinate(String valueStr, String directionStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }

        try {
            BigDecimal value = new BigDecimal(valueStr);

            // Se la direzione è Sud (S) o Ovest (W), il valore diventa negativo
            if ("S".equalsIgnoreCase(directionStr) || "W".equalsIgnoreCase(directionStr)) {
                value = value.negate();
            }

            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            // Format: "2025-11-20 12:30:00"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            // Fallback to current time if parsing fails
            return LocalDateTime.now();
        }
    }

    public long getFireballCount() {
        return fireballRepository.count();
    }
}