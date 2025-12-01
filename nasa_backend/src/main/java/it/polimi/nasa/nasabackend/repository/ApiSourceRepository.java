package it.polimi.nasa.nasabackend.repository;

import it.polimi.nasa.nasabackend.entity.ApiSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiSourceRepository extends JpaRepository<ApiSource, Long> {

    Optional<ApiSource> findByApiName(String apiName);

    boolean existsByApiName(String apiName);
}