package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "geomagnetic_storm", indexes = {
        @Index(name = "idx_gst_start", columnList = "startTime"),
        @Index(name = "idx_gst_kp", columnList = "kpIndex")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeomagneticStorm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String activityId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(precision = 3, scale = 1)
    private BigDecimal kpIndex;

    private Integer dstIndex;

    private Integer gScale;

    @Column(columnDefinition = "TEXT")
    private String instruments;

    // ============================================
    // NUOVI CAMPI - Kp Evolution & Linked Events
    // ============================================
    @Column(name = "all_kp_index", columnDefinition = "TEXT")
    private String allKpIndex; // JSON array con tutti i Kp e timestamps

    @Column(name = "linked_events", columnDefinition = "TEXT")
    private String linkedEvents; // JSON array di CME/IPS collegati

    @Column(name = "submission_time")
    private LocalDateTime submissionTime;

    @Column(name = "version_id")
    private Integer versionId;

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