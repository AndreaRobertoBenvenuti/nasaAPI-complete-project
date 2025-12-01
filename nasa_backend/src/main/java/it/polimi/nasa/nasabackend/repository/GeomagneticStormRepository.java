package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.GeomagneticStorm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeomagneticStormRepository extends JpaRepository<GeomagneticStorm, Long> {

    Optional<GeomagneticStorm> findByActivityId(String activityId);

    boolean existsByActivityId(String activityId);

    List<GeomagneticStorm> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    // Corretto per usare BigDecimal come da Entity
    @Query("SELECT g FROM GeomagneticStorm g WHERE g.kpIndex >= :minKp ORDER BY g.kpIndex DESC")
    List<GeomagneticStorm> findMajorStorms(BigDecimal minKp);

    @Query("SELECT g.startTime FROM GeomagneticStorm g ORDER BY g.startTime DESC LIMIT 1")
    LocalDateTime findLastEventDate();

    // Filtro per scala G (G1...G5)
    @Query("SELECT g FROM GeomagneticStorm g WHERE g.gScale >= :minGScale")
    List<GeomagneticStorm> findByGScale(Integer minGScale);

    // Utile per l'algoritmo di correlazione
    @Query("SELECT g FROM GeomagneticStorm g WHERE g.startTime BETWEEN :cmeTime AND :cmeTimePlus96h")
    List<GeomagneticStorm> findStormAfterCme(LocalDateTime cmeTime, LocalDateTime cmeTimePlus96h);
}