package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.CoronalMassEjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoronalMassEjectionRepository extends JpaRepository<CoronalMassEjection, Long> {

    Optional<CoronalMassEjection> findByActivityId(String activityId);

    boolean existsByActivityId(String activityId);

    List<CoronalMassEjection> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM CoronalMassEjection c WHERE c.speedKmS >= :minSpeed ORDER BY c.speedKmS DESC")
    List<CoronalMassEjection> findFastCme(BigDecimal minSpeed);


    @Query("SELECT c.startTime FROM CoronalMassEjection c ORDER BY c.startTime DESC LIMIT 1")
    LocalDateTime findLastEventDate();

    // Utile per l'algoritmo di correlazione (Cercare CME scaturiti subito dopo un Flare)
    @Query("SELECT c FROM CoronalMassEjection c WHERE c.startTime BETWEEN :flareTime AND :flareTimePlus72h")
    List<CoronalMassEjection> findCmeAfterFlare(LocalDateTime flareTime, LocalDateTime flareTimePlus72h);
}