package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.NeoAsteroid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NeoAsteroidRepository extends JpaRepository<NeoAsteroid, Long> {

    Optional<NeoAsteroid> findByNeoReferenceId(String neoReferenceId);

    boolean existsByNeoReferenceId(String neoReferenceId);

    List<NeoAsteroid> findByIsPotentiallyHazardous(Boolean isHazardous);

    @Query("SELECT n FROM NeoAsteroid n WHERE n.isPotentiallyHazardous = true ORDER BY n.estimatedDiameterKmMax DESC")
    List<NeoAsteroid> findHazardousAsteroidsBySize();

    @Query("SELECT n FROM NeoAsteroid n ORDER BY n.absoluteMagnitudeH ASC")
    List<NeoAsteroid> findBrightestAsteroids();
}