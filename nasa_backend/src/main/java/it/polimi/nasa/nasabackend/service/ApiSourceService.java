package it.polimi.nasa.nasabackend.service;

import it.polimi.nasa.nasabackend.entity.ApiSource;
import it.polimi.nasa.nasabackend.repository.ApiSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApiSourceService {

    @Autowired
    private ApiSourceRepository apiSourceRepository;

    public ApiSource getOrCreateApiSource(String apiName, String apiUrl, String description) {
        return apiSourceRepository.findByApiName(apiName)
                .orElseGet(() -> {
                    ApiSource newSource = new ApiSource();
                    newSource.setApiName(apiName);
                    newSource.setApiUrl(apiUrl);
                    newSource.setDescription(description);
                    newSource.setLastUpdate(LocalDateTime.now());
                    newSource.setTotalRecords(0);
                    return apiSourceRepository.save(newSource);
                });
    }

    public void updateApiSourceStats(String apiName, int recordsAdded) {
        apiSourceRepository.findByApiName(apiName).ifPresent(source -> {
            source.setLastUpdate(LocalDateTime.now());
            source.setTotalRecords(source.getTotalRecords() + recordsAdded);
            apiSourceRepository.save(source);
        });
    }
}