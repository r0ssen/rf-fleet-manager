package dk.rfg.fleetmanager.entity;
import java.io.Serializable;
public record VehicleId(int festivalYear, int vehicleId) implements Serializable {}
