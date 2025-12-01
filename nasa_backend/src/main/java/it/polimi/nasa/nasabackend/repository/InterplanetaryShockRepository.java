package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.InterplanetaryShock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterplanetaryShockRepository extends JpaRepository<InterplanetaryShock, Long> {

    boolean existsByActivityId(String activityId);

    Optional<InterplanetaryShock> findByActivityId(String activityId);

    // Metodo standard JPA per ottenere tutti gli shock ordinati per data (Spring lo implementa da solo)
    // Questo è quello usato dal metodo "getAllIps" nel Service
    List<InterplanetaryShock> findAllByOrderByActivityTimeDesc();

    // Query Custom: Cerca shock che avvengono vicino alla Terra.
    // Usiamo LIKE perché a volte la location è "Earth, L1" o simili.
    // Questo risolve l'errore "cannot resolve method getEarthShocks"
    @Query("SELECT ips FROM InterplanetaryShock ips WHERE ips.location LIKE '%Earth%' ORDER BY ips.activityTime DESC")
    List<InterplanetaryShock> findEarthShocks();

    @Query("SELECT i.activityTime FROM InterplanetaryShock i ORDER BY i.activityTime DESC LIMIT 1")
    LocalDateTime findLastEventDate();

    // Utile per l'algoritmo di correlazione: cerca shock in una finestra temporale specifica dopo un CME
    @Query("SELECT ips FROM InterplanetaryShock ips " +
            "WHERE ips.activityTime >= :startTime AND ips.activityTime <= :endTime " +
            "ORDER BY ips.activityTime")
    List<InterplanetaryShock> findShockAfterCme(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}