package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fireball", indexes = {
        @Index(name = "idx_fireball_date", columnList = "eventDate"),
        @Index(name = "idx_fireball_location", columnList = "latitude,longitude")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fireball {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    // Geographic data
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(precision = 8, scale = 2)
    private BigDecimal altitudeKm;

    // Physical properties
    @Column(precision = 15, scale = 2)
    private BigDecimal totalRadiatedEnergyJ;

    @Column(precision = 10, scale = 4)
    private BigDecimal totalImpactEnergyKt;

    @Column(precision = 8, scale = 2)
    private BigDecimal velocityKmS;

    // Velocity components
    @Column(precision = 10, scale = 2)
    private BigDecimal velocityVx;

    @Column(precision = 10, scale = 2)
    private BigDecimal velocityVy;

    @Column(precision = 10, scale = 2)
    private BigDecimal velocityVz;

    // Metadata
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