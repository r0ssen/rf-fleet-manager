package dk.rfg.fleetmanager.repository;

import dk.rfg.fleetmanager.entity.VehicleAvailability;
import dk.rfg.fleetmanager.entity.VehicleAvailabilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface VehicleAvailabilityRepository
        extends JpaRepository<VehicleAvailability, VehicleAvailabilityId> {

    List<VehicleAvailability> findByFestivalYearAndVehicleId(int festivalYear, int vehicleId);

    @Transactional
    @Modifying
    @Query("DELETE FROM VehicleAvailability va WHERE va.festivalYear = :year AND va.vehicleId = :vehicleId")
    void deleteByFestivalYearAndVehicleId(@Param("year") int year, @Param("vehicleId") int vehicleId);
}

