package dk.rfg.fleetmanager.repository;
import dk.rfg.fleetmanager.entity.Vehicle;
import dk.rfg.fleetmanager.entity.VehicleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, VehicleId> {
    List<Vehicle> findByFestivalYearOrderByVehicleNoAsc(int festivalYear);

    /** Vehicles available for a date: no rows = all days, any rows = only matching dates. */
    @Query("""
           SELECT v FROM Vehicle v
           WHERE v.festivalYear = :year
             AND (
               NOT EXISTS (SELECT 1 FROM VehicleAvailability va
                           WHERE va.festivalYear = :year AND va.vehicleId = v.vehicleId)
               OR EXISTS  (SELECT 1 FROM VehicleAvailability va
                           WHERE va.festivalYear = :year AND va.vehicleId = v.vehicleId
                             AND va.festivalDate = :date)
             )
           ORDER BY v.vehicleNo
           """)
    List<Vehicle> findAvailableForDate(@Param("year") int year, @Param("date") LocalDate date);
}
