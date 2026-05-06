package dk.rfg.fleetmanager.controller;
import dk.rfg.fleetmanager.config.AppConfig;
import dk.rfg.fleetmanager.entity.Vehicle;
import dk.rfg.fleetmanager.entity.VehicleAvailability;
import dk.rfg.fleetmanager.repository.OpeningHoursRepository;
import dk.rfg.fleetmanager.repository.VehicleAvailabilityRepository;
import dk.rfg.fleetmanager.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;
    private final AppConfig appConfig;
    private final OpeningHoursRepository openingHoursRepository;
    private final VehicleAvailabilityRepository availabilityRepository;

    public VehicleController(VehicleService vehicleService, AppConfig appConfig,
                             OpeningHoursRepository openingHoursRepository,
                             VehicleAvailabilityRepository availabilityRepository) {
        this.vehicleService = vehicleService;
        this.appConfig = appConfig;
        this.openingHoursRepository = openingHoursRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
        return "vehicles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        populateAvailabilityModel(0, model);
        return "vehicles/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Vehicle vehicle, BindingResult result,
                         @RequestParam(value = "availableDates", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> availableDates,
                         Model model) {
        if (result.hasErrors()) { populateAvailabilityModel(0, model); return "vehicles/form"; }
        int year = appConfig.getFestivalYear();
        vehicle.setFestivalYear(year);
        Vehicle saved = vehicleService.save(vehicle);
        saveAvailability(year, saved.getVehicleId(), availableDates);
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model model) {
        model.addAttribute("vehicle", vehicleService.getById(appConfig.getFestivalYear(), id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id)));
        populateAvailabilityModel(id, model);
        return "vehicles/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable int id, @Valid @ModelAttribute Vehicle vehicle, BindingResult result,
                         @RequestParam(value = "availableDates", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> availableDates,
                         Model model) {
        if (result.hasErrors()) { populateAvailabilityModel(id, model); return "vehicles/form"; }
        int year = appConfig.getFestivalYear();
        vehicle.setFestivalYear(year);
        vehicle.setVehicleId(id);
        vehicleService.save(vehicle);
        saveAvailability(year, id, availableDates);
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) {
        vehicleService.delete(appConfig.getFestivalYear(), id);
        return "redirect:/vehicles";
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void populateAvailabilityModel(int vehicleId, Model model) {
        int year = appConfig.getFestivalYear();
        // All opening-hours days for this year, sorted
        var allDays = openingHoursRepository.findAll().stream()
                .filter(h -> h.getFestivalYear() == year)
                .sorted((a, b) -> a.getFestivalDate().compareTo(b.getFestivalDate()))
                .toList();
        // Currently checked days for this vehicle
        Set<LocalDate> checkedDates = availabilityRepository
                .findByFestivalYearAndVehicleId(year, vehicleId)
                .stream()
                .map(VehicleAvailability::getFestivalDate)
                .collect(Collectors.toSet());
        model.addAttribute("allDays", allDays);
        model.addAttribute("checkedDates", checkedDates);
    }

    private void saveAvailability(int year, int vehicleId, List<LocalDate> dates) {
        availabilityRepository.deleteByFestivalYearAndVehicleId(year, vehicleId);
        if (dates != null && !dates.isEmpty()) {
            List<VehicleAvailability> rows = dates.stream()
                    .map(d -> new VehicleAvailability(year, vehicleId, d))
                    .toList();
            availabilityRepository.saveAll(rows);
        }
    }
}
