package it.polimi.nasa.nasabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FireballDto {
    private Long id;
    private LocalDateTime eventDate;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitudeKm;
    private BigDecimal totalImpactEnergyKt;
    private BigDecimal velocityKmS;
}