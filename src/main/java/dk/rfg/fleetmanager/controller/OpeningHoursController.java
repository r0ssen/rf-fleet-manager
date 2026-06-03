package dk.rfg.fleetmanager.controller;

import dk.rfg.fleetmanager.config.AppConfig;
import dk.rfg.fleetmanager.entity.OpeningHours;
import dk.rfg.fleetmanager.entity.OpeningHoursId;
import dk.rfg.fleetmanager.repository.OpeningHoursRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/opening-hours")
public class OpeningHoursController {

    private final OpeningHoursRepository repo;
    private final AppConfig appConfig;

    public OpeningHoursController(OpeningHoursRepository repo, AppConfig appConfig) {
        this.repo = repo;
        this.appConfig = appConfig;
    }

    @GetMapping
    public String list(Model model) {
        int year = appConfig.getFestivalYear();
        List<OpeningHours> hours = repo.findAll().stream()
            .filter(h -> h.getFestivalYear() == year)
            .sorted((a, b) -> a.getFestivalDate().compareTo(b.getFestivalDate()))
            .toList();
        model.addAttribute("hoursList", hours);
        model.addAttribute("year", year);
        return "opening-hours/list";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate festivalDate,
            @RequestParam String openFrom,
            @RequestParam String openUntil,
            Model model) {

        LocalTime openFromTime = LocalTime.parse(openFrom);
        LocalTime openUntilTime = LocalTime.parse(openUntil);

        // Validate that closing time is after opening time
        if (!openUntilTime.isAfter(openFromTime)) {
            List<OpeningHours> hours = repo.findAll().stream()
                .filter(h -> h.getFestivalYear() == appConfig.getFestivalYear())
                .sorted((a, b) -> a.getFestivalDate().compareTo(b.getFestivalDate()))
                .toList();
            model.addAttribute("hoursList", hours);
            model.addAttribute("year", appConfig.getFestivalYear());
            model.addAttribute("errorMessage", "Lukketid skal være efter åbningstid.");
            return "opening-hours/list";
        }

        int year = appConfig.getFestivalYear();
        OpeningHours oh = repo.findById(new OpeningHoursId(year, festivalDate))
            .orElse(new OpeningHours());
        oh.setFestivalYear(year);
        oh.setFestivalDate(festivalDate);
        oh.setOpenFrom(openFromTime);
        oh.setOpenUntil(openUntilTime);
        repo.save(oh);
        return "redirect:/opening-hours";
    }

    @PostMapping("/delete")
    public String delete(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate festivalDate) {
        repo.deleteById(new OpeningHoursId(appConfig.getFestivalYear(), festivalDate));
        return "redirect:/opening-hours";
    }
}
