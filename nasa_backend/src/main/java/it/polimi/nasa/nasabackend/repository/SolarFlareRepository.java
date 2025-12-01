package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.SolarFlare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolarFlareRepository extends JpaRepository<SolarFlare, Long> {

    Optional<SolarFlare> findByActivityId(String activityId);

    boolean existsByActivityId(String activityId);

    List<SolarFlare> findByPeakTimeBetween(LocalDateTime start, LocalDateTime end);

    List<SolarFlare> findByClassType(String classType);

    @Query("SELECT f FROM SolarFlare f WHERE f.classType IN ('M', 'X') ORDER BY f.peakTime DESC")
    List<SolarFlare> findMajorFlares();

    @Query("SELECT f.peakTime FROM SolarFlare f ORDER BY f.peakTime DESC LIMIT 1")
    LocalDateTime findLastEventDate();
    @Query("SELECT f FROM SolarFlare f WHERE f.classType = :classType AND f.peakTime BETWEEN :start AND :end")
    List<SolarFlare> findByClassTypeAndDateRange(String classType, LocalDateTime start, LocalDateTime end);
}