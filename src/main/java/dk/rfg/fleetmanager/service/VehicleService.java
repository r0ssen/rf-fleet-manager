package dk.rfg.fleetmanager.service;
import dk.rfg.fleetmanager.entity.Vehicle;
import dk.rfg.fleetmanager.entity.VehicleId;
import dk.rfg.fleetmanager.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository repo;
    public VehicleService(VehicleRepository repo) { this.repo = repo; }

    public List<Vehicle> getAllVehicles(int year) { return repo.findByFestivalYearOrderByVehicleNoAsc(year); }
    public boolean vehicleNoExists(int year, String vehicleNo, int excludeId) {
        return repo.existsByFestivalYearAndVehicleNoAndVehicleIdNot(year, vehicleNo, excludeId);
    }
    public List<Vehicle> getAvailableVehiclesForDate(int year, LocalDate date) { return repo.findAvailableForDate(year, date); }
    public Optional<Vehicle> getById(int year, int id) { return repo.findById(new VehicleId(year, id)); }

    @Transactional public Vehicle save(Vehicle v) { return repo.save(v); }
    @Transactional public void delete(int year, int id) { repo.deleteById(new VehicleId(year, id)); }
}
