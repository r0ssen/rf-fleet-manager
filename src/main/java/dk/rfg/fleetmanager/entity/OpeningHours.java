package dk.rfg.fleetmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "opening_hours")
@IdClass(OpeningHoursId.class)
public class OpeningHours {

    @Id @Column(name = "festival_year") private int festivalYear;
    @Id @Column(name = "festival_date") private LocalDate festivalDate;
    @Column(name = "open_from",  nullable = false) private LocalTime openFrom;
    @Column(name = "open_until", nullable = false) private LocalTime openUntil;

    public OpeningHours() {}

    public int getFestivalYear()        { return festivalYear; }
    public void setFestivalYear(int v)  { this.festivalYear = v; }
    public LocalDate getFestivalDate()          { return festivalDate; }
    public void setFestivalDate(LocalDate v)    { this.festivalDate = v; }
    public LocalTime getOpenFrom()              { return openFrom; }
    public void setOpenFrom(LocalTime v)        { this.openFrom = v; }
    public LocalTime getOpenUntil()             { return openUntil; }
    public void setOpenUntil(LocalTime v)       { this.openUntil = v; }
}
