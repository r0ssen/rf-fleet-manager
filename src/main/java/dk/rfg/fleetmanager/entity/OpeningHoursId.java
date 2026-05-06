package dk.rfg.fleetmanager.entity;
import java.io.Serializable;
import java.time.LocalDate;
public record OpeningHoursId(int festivalYear, LocalDate festivalDate) implements Serializable {}
