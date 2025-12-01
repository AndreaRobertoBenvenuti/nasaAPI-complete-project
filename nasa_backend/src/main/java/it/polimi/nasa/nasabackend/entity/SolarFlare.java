package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solar_flare", indexes = {
        @Index(name = "idx_flare_peak", columnList = "peakTime"),
        @Index(name = "idx_flare_class", columnList = "classType,classIntensity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolarFlare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String activityId;

    // Timing
    private LocalDateTime beginTime;

    @Column(nullable = false)
    private LocalDateTime peakTime;

    private LocalDateTime endTime;

    // Classification
    @Column(nullable = false, length = 2)
    private String classType; // B, C, M, X

    @Column(precision = 6, scale = 2)
    private BigDecimal classIntensity;

    @Column(length = 10)
    private String fullClass; // "X2.5"

    // Location on Sun
    @Column(length = 10)
    private String sourceLocation; // "S20W30"

    private Integer activeRegionNum;

    // Instruments (stored as comma-separated)
    @Column(columnDefinition = "TEXT")
    private String instruments;

    // ============================================
    // NUOVI CAMPI - Linked Events & Metadata
    // ============================================
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "linked_events", columnDefinition = "TEXT")
    private String linkedEvents; // JSON array di CME/particle events collegati

    @Column(name = "submission_time")
    private LocalDateTime submissionTime;

    @Column(name = "version_id")
    private Integer versionId;

    // ============================================

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