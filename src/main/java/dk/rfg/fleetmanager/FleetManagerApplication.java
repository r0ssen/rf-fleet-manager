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
}
