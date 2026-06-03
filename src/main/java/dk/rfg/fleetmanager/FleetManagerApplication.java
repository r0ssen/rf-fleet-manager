package dk.rfg.fleetmanager;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class FleetManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetManagerApplication.class, args);
    }

    @PostConstruct
    public void checkFlyway() {
        var resource = getClass().getClassLoader().getResource("db/migration/V1__init_schema.sql");
        System.out.println(">>> Migration file in classpath: " + resource);
        System.out.println(">>> Flyway enabled property: " + env.getProperty("spring.flyway.enabled"));
        System.out.println(">>> Flyway locations property: " + env.getProperty("spring.flyway.locations"));
    }

    @Autowired
    private Environment env;
}
