package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "neo_close_approach", indexes = {
        @Index(name = "idx_approach_date", columnList = "approachDate"),
        @Index(name = "idx_approach_neo", columnList = "neo_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeoCloseApproach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neo_id", nullable = false)
    private NeoAsteroid neo;

    @Column(nullable = false)
    private LocalDateTime approachDate;

    // Distance data
    @Column(precision = 15, scale = 2)
    private BigDecimal missDistanceKm;

    @Column(precision = 12, scale = 8)
    private BigDecimal missDistanceAu;

    @Column(precision = 10, scale = 2)
    private BigDecimal missDistanceLunar;

    // Velocity
    @Column(precision = 10, scale = 2)
    private BigDecimal relativeVelocityKmS;

    @Column(precision = 12, scale = 2)
    private BigDecimal relativeVelocityKmH;

    @Column(length = 20)
    private String orbitingBody = "Earth";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}