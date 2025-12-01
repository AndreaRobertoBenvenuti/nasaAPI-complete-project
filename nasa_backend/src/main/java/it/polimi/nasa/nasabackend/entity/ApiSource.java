package it.polimi.nasa.nasabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_source")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String apiName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String apiUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime lastUpdate;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer totalRecords;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}