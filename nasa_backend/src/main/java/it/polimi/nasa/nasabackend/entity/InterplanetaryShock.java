package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interplanetary_shock", indexes = {
        @Index(name = "idx_ips_time", columnList = "activityTime"),
        @Index(name = "idx_ips_location", columnList = "location")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterplanetaryShock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String activityId;

    @Column(length = 50)
    private String catalog;

    @Column(nullable = false)
    private LocalDateTime activityTime;

    @Column(length = 50)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String instruments;

    @Column(name = "linked_events", columnDefinition = "TEXT")
    private String linkedEvents; // JSON array

    @Column(name = "submission_time")
    private LocalDateTime submissionTime;

    @Column(name = "version_id")
    private Integer versionId;

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