package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coronal_mass_ejection", indexes = {
        @Index(name = "idx_cme_time", columnList = "startTime"),
        @Index(name = "idx_cme_speed", columnList = "speedKmS")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoronalMassEjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String activityId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal speedKmS;

    @Column(precision = 6, scale = 2)
    private BigDecimal halfAngleDeg;

    @Column(length = 20)
    private String type;

    @Column(length = 10)
    private String sourceLocation;

    @Column(precision = 6, scale = 2)
    private BigDecimal latitude;

    @Column(precision = 6, scale = 2)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String instruments;

    @Column(columnDefinition = "TEXT")
    private String note;

    // ============================================
    // NUOVI CAMPI - CME Analysis Details
    // ============================================
    @Column(name = "is_most_accurate")
    private Boolean isMostAccurate = false;

    @Column(name = "time_21_5")
    private LocalDateTime time215;

    @Column(name = "feature_code", length = 10)
    private String featureCode;

    @Column(name = "image_type", length = 50)
    private String imageType;

    @Column(name = "measurement_technique", length = 100)
    private String measurementTechnique;

    @Column(name = "level_of_data")
    private Integer levelOfData;

    @Column(precision = 10, scale = 2)
    private BigDecimal tilt;

    @Column(name = "minor_half_width", precision = 10, scale = 2)
    private BigDecimal minorHalfWidth;

    @Column(name = "speed_measured_at_height", precision = 10, scale = 2)
    private BigDecimal speedMeasuredAtHeight;

    // Metadata
    @Column(name = "submission_time")
    private LocalDateTime submissionTime;

    @Column(name = "version_id")
    private Integer versionId;

    @Column(name = "linked_events", columnDefinition = "TEXT")
    private String linkedEvents; // JSON array

    // ============================================

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