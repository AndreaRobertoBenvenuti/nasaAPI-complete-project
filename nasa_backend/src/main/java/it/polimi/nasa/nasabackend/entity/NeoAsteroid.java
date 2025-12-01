package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "neo_asteroid", indexes = {
        @Index(name = "idx_neo_hazardous", columnList = "isPotentiallyHazardous"),
        @Index(name = "idx_neo_magnitude", columnList = "absoluteMagnitudeH")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeoAsteroid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String neoReferenceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean isPotentiallyHazardous = false;

    // ============================================
    // NUOVO CAMPO - Sentry Object
    // ============================================
    @Column(name = "is_sentry_object")
    private Boolean isSentryObject = false;

    // ============================================

    @Column(precision = 6, scale = 2)
    private BigDecimal absoluteMagnitudeH;

    // Size estimates
    @Column(precision = 10, scale = 4)
    private BigDecimal estimatedDiameterKmMin;

    @Column(precision = 10, scale = 4)
    private BigDecimal estimatedDiameterKmMax;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedDiameterMMin;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedDiameterMMax;

    // Orbital data
    @Column(precision = 12, scale = 2)
    private BigDecimal orbitalPeriodDays;

    @Column(precision = 10, scale = 8)
    private BigDecimal orbitEccentricity;

    @Column(precision = 12, scale = 8)
    private BigDecimal semiMajorAxisAu;

    @Column(precision = 8, scale = 4)
    private BigDecimal inclinationDeg;

    // Observation dates
    private LocalDate firstObservationDate;
    private LocalDate lastObservationDate;

    @Column(columnDefinition = "TEXT")
    private String nasaJplUrl;

    @ManyToOne
    @JoinColumn(name = "api_source_id")
    private ApiSource apiSource;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}