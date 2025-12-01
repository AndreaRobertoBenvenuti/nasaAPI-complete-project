package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.NeoCloseApproach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NeoCloseApproachRepository extends JpaRepository<NeoCloseApproach, Long> {

    List<NeoCloseApproach> findByApproachDateBetween(LocalDateTime start, LocalDateTime end);

    // JOIN FETCH è ottimo per le performance qui (evita N+1 query)
    @Query("SELECT nca FROM NeoCloseApproach nca JOIN FETCH nca.neo WHERE nca.approachDate BETWEEN :start AND :end")
    List<NeoCloseApproach> findApproachesWithNeoByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT nca FROM NeoCloseApproach nca ORDER BY nca.missDistanceKm ASC")
    List<NeoCloseApproach> findClosestApproaches();

    @Query("SELECT n.approachDate FROM NeoCloseApproach n ORDER BY n.approachDate DESC LIMIT 1")
    LocalDateTime findLastEventDate();

    @Query("SELECT nca FROM NeoCloseApproach nca WHERE nca.neo.isPotentiallyHazardous = true AND nca.approachDate >= :now")
    List<NeoCloseApproach> findUpcomingHazardousApproaches(LocalDateTime now);
}