package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.Fireball;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FireballRepository extends JpaRepository<Fireball, Long> {

    List<Fireball> findByEventDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT f FROM Fireball f ORDER BY f.totalImpactEnergyKt DESC")
    List<Fireball> findTopByEnergyDesc();

    @Query("SELECT f.eventDate FROM Fireball f ORDER BY f.eventDate DESC LIMIT 1")
    LocalDateTime findLastEventDate();

    @Query("SELECT COUNT(f) FROM Fireball f WHERE f.eventDate BETWEEN :start AND :end")
    Long countByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT f FROM Fireball f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL")
    List<Fireball> findAllWithLocation();
}