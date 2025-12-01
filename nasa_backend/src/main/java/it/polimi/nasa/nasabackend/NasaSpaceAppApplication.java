package it.polimi.nasa.nasabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NasaSpaceAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(NasaSpaceAppApplication.class, args);
        System.out.println("🚀 NASA Space Events Dashboard - RUNNING");
        System.out.println("📊 API Base: http://localhost:8080/api");
    }
}