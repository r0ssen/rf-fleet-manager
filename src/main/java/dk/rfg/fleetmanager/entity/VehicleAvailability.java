package dk.rfg.fleetmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vehicle_availability")
@IdClass(VehicleAvailabilityId.class)
public class VehicleAvailability {

    @Id @Column(name = "festival_year") private int festivalYear;
    @Id @Column(name = "vehicle_id")    private int vehicleId;
    @Id @Column(name = "festival_date") private LocalDate festivalDate;

    public VehicleAvailability() {}

    public VehicleAvailability(int festivalYear, int vehicleId, LocalDate festivalDate) {
        this.festivalYear = festivalYear;
        this.vehicleId    = vehicleId;
        this.festivalDate = festivalDate;
    }

    public int getFestivalYear()            { return festivalYear; }
    public void setFestivalYear(int v)      { this.festivalYear = v; }
    public int getVehicleId()               { return vehicleId; }
    public void setVehicleId(int v)         { this.vehicleId = v; }
    public LocalDate getFestivalDate()      { return festivalDate; }
    public void setFestivalDate(LocalDate v){ this.festivalDate = v; }
}

