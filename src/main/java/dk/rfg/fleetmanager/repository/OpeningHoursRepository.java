package dk.rfg.fleetmanager.repository;

import dk.rfg.fleetmanager.entity.OpeningHours;
import dk.rfg.fleetmanager.entity.OpeningHoursId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, OpeningHoursId> {
    Optional<OpeningHours> findByFestivalYearAndFestivalDate(int festivalYear, LocalDate date);
}
