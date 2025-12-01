package it.polimi.nasa.nasabackend.config;

import it.polimi.nasa.nasabackend.repository.*;
import it.polimi.nasa.nasabackend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataLoaderConfig {

    // Services
    @Autowired private SolarFlareService solarFlareService;
    @Autowired private CoronalMassEjectionService cmeService;
    @Autowired private GeomagneticStormService stormService;
    @Autowired private InterplanetaryShockService ipsService;
    @Autowired private FireballService fireballService;
    @Autowired private NeoService neoService;

    // Repositories (Per controllare l'ultima data)
    @Autowired private SolarFlareRepository flareRepository;
    @Autowired private CoronalMassEjectionRepository cmeRepository;
    @Autowired private GeomagneticStormRepository stormRepository;
    @Autowired private InterplanetaryShockRepository ipsRepository;
    @Autowired private NeoCloseApproachRepository neoCloseApproachRepository;

    // Configurazione finestra temporale
    private static final int DEFAULT_YEARS_BACK = 3; // Per tutti gli eventi solari
    private static final int DEFAULT_WEEKS_BACK_NEO = 4; // Solo per gli asteroidi (più pesante)

    @Bean
    public CommandLineRunner loadInitialData() {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("🚀 NASA SMART DATA LOADER - Starting...");
            System.out.println("========================================\n");

            String todayStr = LocalDate.now().toString();

            // Data di default per Solar Flares, CME, Storms (3 Anni fa)
            String defaultGeneralStartStr = LocalDate.now().minusYears(DEFAULT_YEARS_BACK).toString();

            // Data di default SOLO per Asteroidi (4 Settimane fa)
            String defaultNeoStartStr = LocalDate.now().minusWeeks(DEFAULT_WEEKS_BACK_NEO).toString();

            try {
                // 1. Solar Flares (Usa 3 anni)
                String flareStart = getStartDate(flareRepository.findLastEventDate(), defaultGeneralStartStr);
                if (shouldFetch(flareStart)) {
                    System.out.println("☀️ Checking Solar Flares (" + flareStart + " -> " + todayStr + ")...");
                    solarFlareService.fetchAndSaveFlares(flareStart, todayStr);
                } else {
                    System.out.println("✅ Solar Flares are up to date.");
                }

                // 2. CME (Usa 3 anni)
                String cmeStart = getStartDate(cmeRepository.findLastEventDate(), defaultGeneralStartStr);
                if (shouldFetch(cmeStart)) {
                    System.out.println("🌊 Checking CMEs (" + cmeStart + " -> " + todayStr + ")...");
                    cmeService.fetchAndSaveCme(cmeStart, todayStr);
                } else {
                    System.out.println("✅ CMEs are up to date.");
                }

                // 3. Interplanetary Shocks (Usa 3 anni)
                String ipsStart = getStartDate(ipsRepository.findLastEventDate(), defaultGeneralStartStr);
                if (shouldFetch(ipsStart)) {
                    System.out.println("💥 Checking IP Shocks (" + ipsStart + " -> " + todayStr + ")...");
                    ipsService.fetchAndSaveIps(ipsStart, todayStr);
                } else {
                    System.out.println("✅ IP Shocks are up to date.");
                }

                // 4. Geomagnetic Storms (Usa 3 anni)
                String stormStart = getStartDate(stormRepository.findLastEventDate(), defaultGeneralStartStr);
                if (shouldFetch(stormStart)) {
                    System.out.println("🌍 Checking Geomagnetic Storms (" + stormStart + " -> " + todayStr + ")...");
                    stormService.fetchAndSaveStorms(stormStart, todayStr);
                } else {
                    System.out.println("✅ Geomagnetic Storms are up to date.");
                }

                // 5. Fireballs
                System.out.println("☄️ Checking Fireball Events...");
                fireballService.fetchAndSaveFireballs();

                // 6. NEO Asteroids (Usa 4 SETTIMANE)
                LocalDateTime lastNeoDate = neoCloseApproachRepository.findLastEventDate();

                // Qui passiamo 'defaultNeoStartStr' invece di quello generale
                String neoStart = getStartDate(lastNeoDate, defaultNeoStartStr);

                if (shouldFetch(neoStart)) {
                    System.out.println("🪐 Checking NEO Asteroids (" + neoStart + " -> " + todayStr + ")...");
                    neoService.fetchAndSaveNeo(neoStart, todayStr);
                } else {
                    System.out.println("✅ NEO Asteroids are up to date.");
                }

                System.out.println("\n========================================");
                System.out.println("✅ SMART SYNC COMPLETED!");
                System.out.println("========================================");

                printSummary();

            } catch (Exception e) {
                System.err.println("\n❌ ERROR during smart data loading: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    /**
     * Calcola la data di inizio download.
     * Se esiste una data nel DB, riparte da quella (meno 1 giorno per sicurezza).
     * Se non esiste, usa la data di default passata come parametro.
     */
    private String getStartDate(LocalDateTime lastEventDate, String defaultStart) {
        if (lastEventDate == null) {
            return defaultStart;
        }
        return lastEventDate.minusDays(1).toLocalDate().toString();
    }

    private boolean shouldFetch(String startDateStr) {
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate today = LocalDate.now();
        return start.isBefore(today);
    }

    private void printSummary() {
        System.out.println("\n📈 DATA SUMMARY (Current DB State):");
        System.out.println("├─ Solar Flares: " + solarFlareService.getFlareCount());
        System.out.println("├─ CME Events: " + cmeService.getCmeCount());
        System.out.println("├─ Interplanetary Shocks: " + ipsService.getIpsCount());
        System.out.println("├─ Geomagnetic Storms: " + stormService.getStormCount());
        System.out.println("├─ Fireball Events: " + fireballService.getFireballCount());
        System.out.println("└─ NEO Asteroids: " + neoService.getAsteroidCount());
        System.out.println("\n🔗 System Ready.");
    }
}