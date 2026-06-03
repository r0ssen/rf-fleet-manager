package dk.rfg.fleetmanager;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FleetManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetManagerApplication.class, args);
    }

    @PostConstruct
    public void checkFlyway() {
        var resource = getClass().getClassLoader().getResource("db/migration/V1__init_schema.sql");
        System.out.println(">>> Flyway migration file found: " + resource);
    }
}
