package dk.rfg.fleetmanager.controller;

import dk.rfg.fleetmanager.config.AppConfig;
import dk.rfg.fleetmanager.service.FleetGridService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/fleet")
public class FleetGridController {

    private final FleetGridService fleetGridService;
    private final AppConfig appConfig;

    public FleetGridController(FleetGridService fleetGridService,
                               AppConfig appConfig) {
        this.fleetGridService = fleetGridService;
        this.appConfig        = appConfig;
    }

    @GetMapping
    public String grid(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Boolean showCancelled,
            @RequestParam(required = false) Boolean hideCancelled,
            Model model) {

        if (date == null) date = LocalDate.now();
        int year = appConfig.getFestivalYear();
        // Backward compatible: old hideCancelled param still works.
        boolean resolvedShowCancelled = showCancelled != null
            ? showCancelled
            : (hideCancelled != null ? !hideCancelled : false);

        FleetGridService.GridData grid = fleetGridService.buildGrid(year, date, !resolvedShowCancelled);

        model.addAttribute("date",        date);
        model.addAttribute("prevDay",     date.minusDays(1));
        model.addAttribute("nextDay",     date.plusDays(1));
        model.addAttribute("rows",        grid.rows());
        model.addAttribute("timeMarkers", grid.timeMarkers());
        model.addAttribute("openFrom",    grid.openFrom());
        model.addAttribute("openUntil",  grid.openUntil());
        model.addAttribute("hoursFromDb", grid.hoursFromDb());
        model.addAttribute("showCancelled", resolvedShowCancelled);
        model.addAttribute("danishLocale", java.util.Locale.forLanguageTag("da"));
        return "fleet/grid";
    }
}
