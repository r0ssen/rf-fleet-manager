package dk.rfg.fleetmanager.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class VehicleAvailabilityId implements Serializable {
    private int festivalYear;
    private int vehicleId;
    private LocalDate festivalDate;

    public VehicleAvailabilityId() {}

    public VehicleAvailabilityId(int festivalYear, int vehicleId, LocalDate festivalDate) {
        this.festivalYear = festivalYear;
        this.vehicleId    = vehicleId;
        this.festivalDate = festivalDate;
    }

    public int getFestivalYear()       { return festivalYear; }
    public int getVehicleId()          { return vehicleId; }
    public LocalDate getFestivalDate() { return festivalDate; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleAvailabilityId v)) return false;
        return festivalYear == v.festivalYear && vehicleId == v.vehicleId && Objects.equals(festivalDate, v.festivalDate);
    }
    @Override public int hashCode() { return Objects.hash(festivalYear, vehicleId, festivalDate); }
}



