package dk.rfg.fleetmanager.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.rfg.fleetmanager.config.AppConfig;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskStatus;
import dk.rfg.fleetmanager.entity.User;
import dk.rfg.fleetmanager.repository.OpeningHoursRepository;
import dk.rfg.fleetmanager.repository.UserRepository;
import dk.rfg.fleetmanager.service.TaskService;
import dk.rfg.fleetmanager.service.VehicleService;
import dk.rfg.fleetmanager.util.DivisionTeamData;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;
    private final VehicleService vehicleService;
    private final AppConfig appConfig;
    private final DivisionTeamData divisionTeamData;
    private final ObjectMapper objectMapper;
    private final OpeningHoursRepository openingHoursRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public TaskController(TaskService taskService, VehicleService vehicleService,
                          AppConfig appConfig, DivisionTeamData divisionTeamData,
                          ObjectMapper objectMapper, OpeningHoursRepository openingHoursRepository,
                          UserRepository userRepository) {
        this.taskService = taskService; this.vehicleService = vehicleService;
        this.appConfig = appConfig; this.divisionTeamData = divisionTeamData;
        this.objectMapper = objectMapper; this.openingHoursRepository = openingHoursRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(defaultValue = "false") boolean showCancelled,
                       @RequestParam(defaultValue = "false") boolean showDone,
                       @RequestParam(defaultValue = "false") boolean showOrdered,
                       Authentication auth, Model model) throws Exception {
        boolean isDriver = isDriver(auth);
        int year = appConfig.getFestivalYear();
        List<Task> tasks;

        if (isDriver) {
            date = LocalDate.now();
            User driver = userRepository.findByUsername(auth.getName()).orElseThrow();
            Long driverId = driver.getId();
            tasks = taskService.getTasksForDay(year, date).stream()
                    .filter(t -> t.getDriverUser() != null && driverId.equals(t.getDriverUser().getId()))
                    .filter(t -> t.getStatus() != TaskStatus.CANCELLED)
                    .filter(t -> showDone || t.getStatus() != TaskStatus.DONE)
                    .toList();
        } else {
            if (date == null) date = LocalDate.now();
            tasks = taskService.getTasksForDay(year, date);
            if (!showCancelled) tasks = tasks.stream().filter(t -> t.getStatus() != TaskStatus.CANCELLED).toList();
            if (!showDone)      tasks = tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).toList();
        }

        model.addAttribute("tasks", tasks);
        model.addAttribute("date", date);
        model.addAttribute("isDriver", isDriver);
        model.addAttribute("statuses", editableStatuses());
        model.addAttribute("prevDay", date.minusDays(1));
        model.addAttribute("nextDay", date.plusDays(1));
        model.addAttribute("showCancelled", showCancelled);
        model.addAttribute("showDone", showDone);
        model.addAttribute("showOrdered", showOrdered);
        model.addAttribute("danishLocale", java.util.Locale.forLanguageTag("da"));
        model.addAttribute("now", OffsetDateTime.now(ZoneId.systemDefault()));
        return "tasks/list";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q,
                         @RequestParam(defaultValue = "false") boolean showCancelled,
                         Authentication auth, Model model) {
        if (isDriver(auth)) return "redirect:/tasks";
        List<Task> results = taskService.search(appConfig.getFestivalYear(), q);
        if (!showCancelled) results = results.stream().filter(t -> t.getStatus() != TaskStatus.CANCELLED).toList();
        LocalDate today = LocalDate.now();
        model.addAttribute("tasks", results);
        model.addAttribute("pastTasks",   results.stream().filter(t -> t.getStartTs() != null && t.getStartTs().toLocalDate().isBefore(today)).toList());
        model.addAttribute("todayTasks",  results.stream().filter(t -> t.getStartTs() != null && t.getStartTs().toLocalDate().isEqual(today)).toList());
        model.addAttribute("futureTasks", results.stream().filter(t -> t.getStartTs() != null && t.getStartTs().toLocalDate().isAfter(today)).toList());
        model.addAttribute("q", q);
        model.addAttribute("showCancelled", showCancelled);
        model.addAttribute("danishLocale", java.util.Locale.forLanguageTag("da"));
        return "tasks/search";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                          @RequestParam(required = false) Integer vehicleId,
                          @RequestParam(required = false, defaultValue = "fleet") String returnTo,
                          Authentication auth, Model model) throws Exception {
        if (isDriver(auth)) return "redirect:/tasks";
        Task task = new Task();
        applyNewTaskPrefill(task, date, start, vehicleId);
        populateForm(task, date, returnTo, model);
        model.addAttribute("isDriver", false);
        return "tasks/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id,
                           @RequestParam(required = false, defaultValue = "fleet") String returnTo,
                           @RequestParam(required = false, defaultValue = "") String searchQuery,
                           Authentication auth, Model model) throws Exception {
        boolean driver = isDriver(auth);
        Task task = taskService.getById(appConfig.getFestivalYear(), id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        if (driver) returnTo = "tasks";
        populateForm(task, null, returnTo, model);
        model.addAttribute("isDriver", driver);
        model.addAttribute("searchQuery", searchQuery);
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Task task, BindingResult result,
                         @RequestParam(required = false, defaultValue = "fleet") String returnTo,
                         @RequestParam(required = false) Long driverId,
                         @RequestParam(required = false, defaultValue = "") String searchQuery,
                         Authentication auth, Model model) throws Exception {
        validateTimeRange(task, result);
        if (result.hasErrors()) {
            populateForm(task, null, returnTo, model);
            model.addAttribute("searchQuery", searchQuery);
            return "tasks/form";
        }
        task.setFestivalYear(appConfig.getFestivalYear());
        task.setReceivedBy(auth.getName());
        if (driverId != null) {
            userRepository.findById(driverId).ifPresent(task::setDriverUser);
        }
        taskService.save(task);
        LocalDate date = task.getStartTs() != null ? task.getStartTs().toLocalDate() : null;
        if ("search".equals(returnTo)) return redirectToSearch(searchQuery);
        return "tasks".equals(returnTo) ? redirectToTaskDate(date) : redirectToFleetDate(date);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable int id, @Valid @ModelAttribute Task task, BindingResult result,
                         @RequestParam(required = false, defaultValue = "fleet") String returnTo,
                         @RequestParam(required = false, defaultValue = "") String statusAfterSave,
                         @RequestParam(required = false) Long driverId,
                         @RequestParam(required = false, defaultValue = "") String searchQuery,
                         Authentication auth, Model model) throws Exception {
        task.setTaskId(id);
        task.setFestivalYear(appConfig.getFestivalYear());
        Task existing = taskService.getById(appConfig.getFestivalYear(), id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        if (existing.getStatus() == TaskStatus.CANCELLED) {
            result.reject("task.cancelled.readonly", "Slettede kørsler kan ikke redigeres. Brug Genåbn først.");
            populateForm(existing, null, returnTo, model);
            model.addAttribute("searchQuery", searchQuery);
            return "tasks/form";
        }
        // Lock vehicle and driver when task is STARTED or DONE
        if (existing.getStatus() == TaskStatus.STARTED || existing.getStatus() == TaskStatus.DONE) {
            task.setVehicleId(existing.getVehicleId());
            task.setDriverUser(existing.getDriverUser());
        } else if (driverId != null) {
            userRepository.findById(driverId).ifPresent(task::setDriverUser);
        }
        task.setStatus(existing.getStatus());
        String receivedBy = (existing.getReceivedBy() != null && !existing.getReceivedBy().isBlank())
                ? existing.getReceivedBy() : auth.getName();
        task.setReceivedBy(receivedBy);
        validateTimeRange(task, result);
        if (result.hasErrors()) {
            populateForm(task, null, returnTo, model);
            model.addAttribute("searchQuery", searchQuery);
            return "tasks/form";
        }
        taskService.save(task);
        if (!statusAfterSave.isBlank()) {
            try {
                TaskStatus targetStatus = TaskStatus.valueOf(statusAfterSave);
                if (isAllowedStatusTransition(existing.getStatus(), targetStatus)) {
                    if (targetStatus != TaskStatus.STARTED
                            || (task.getVehicleId() != null && task.getDriverUser() != null)) {
                        taskService.updateStatus(appConfig.getFestivalYear(), id, targetStatus);
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        LocalDate date = task.getStartTs() != null ? task.getStartTs().toLocalDate() : null;
        if ("search".equals(returnTo)) return redirectToSearch(searchQuery);
        return "tasks".equals(returnTo) ? redirectToTaskDate(date) : redirectToFleetDate(date);
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable int id, @RequestParam TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "fleet") String returnTo,
            @RequestParam(required = false, defaultValue = "false") boolean showCancelled,
            @RequestParam(required = false, defaultValue = "false") boolean showDone,
            RedirectAttributes attrs) {
        LocalDate redirectDate = resolveRedirectDate(id, date);
        Task task = taskService.getById(appConfig.getFestivalYear(), id).orElse(null);
        if (task == null || !isAllowedStatusTransition(task.getStatus(), status)) {
            return "tasks".equals(returnTo)
                ? redirectToTaskDate(redirectDate, showCancelled, showDone)
                : redirectToFleetDate(redirectDate);
        }
        if (status == TaskStatus.STARTED && !hasStartRequirements(task)) {
            attrs.addFlashAttribute("errorMessage", "Du skal vælge både bil og chauffør før en kørsel kan startes.");
            return "tasks".equals(returnTo)
                ? redirectToTaskDate(redirectDate, showCancelled, showDone)
                : redirectToFleetDate(redirectDate);
        }
        taskService.updateStatus(appConfig.getFestivalYear(), id, status);
        return "tasks".equals(returnTo)
            ? redirectToTaskDate(redirectDate, showCancelled, showDone)
            : redirectToFleetDate(redirectDate);
    }

    @PostMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateStatusApi(@PathVariable int id, @RequestParam TaskStatus status) {
        try {
            Task task = taskService.getById(appConfig.getFestivalYear(), id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
            if (status == TaskStatus.STARTED && !hasStartRequirements(task)) {
                return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                    put("error", "Du skal vælge både bil og chauffør før en kørsel kan startes.");
                }});
            }
            if (!isAllowedStatusTransition(task.getStatus(), status)) {
                return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                    put("error", "Ugyldig statusovergang.");
                }});
            }

            Task updated = taskService.updateStatus(appConfig.getFestivalYear(), id, status);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("taskId", updated.getTaskId());
                put("status", updated.getStatus().name());
                put("statusLabel", updated.getStatus().danishLabel());
            }});
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                put("error", ex.getMessage());
            }});
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "fleet") String returnTo,
            @RequestParam(required = false, defaultValue = "false") boolean showCancelled,
            @RequestParam(required = false, defaultValue = "false") boolean showDone) {
        LocalDate redirectDate = resolveRedirectDate(id, date);
        TaskStatus currentStatus = taskService.getById(appConfig.getFestivalYear(), id)
            .map(Task::getStatus)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        TaskStatus nextStatus = currentStatus == TaskStatus.CANCELLED ? TaskStatus.ORDERED : TaskStatus.CANCELLED;
        taskService.updateStatus(appConfig.getFestivalYear(), id, nextStatus);
        return "tasks".equals(returnTo)
            ? redirectToTaskDate(redirectDate, showCancelled, showDone)
            : redirectToFleetDate(redirectDate);
    }

    @PostMapping("/{id}/duplicate")
    public String duplicateFromList(@PathVariable int id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean showCancelled,
            @RequestParam(defaultValue = "false") boolean showDone) {
        Task duplicated = taskService.duplicateWithoutVehicle(appConfig.getFestivalYear(), id);
        LocalDate redirectDate = date != null
            ? date
            : (duplicated.getStartTs() != null ? duplicated.getStartTs().toLocalDate() : LocalDate.now());
        return redirectToTaskDate(redirectDate, showCancelled, showDone);
    }

    @GetMapping("/api/teams")
    @ResponseBody
    public ResponseEntity<List<String>> teamsForDivision(@RequestParam String division) {
        return ResponseEntity.ok(divisionTeamData.getTeamsForDivision(division));
    }

    @GetMapping("/api/opening-hours")
    @ResponseBody
    public ResponseEntity<?> getOpeningHours(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var oh = openingHoursRepository.findByFestivalYearAndFestivalDate(appConfig.getFestivalYear(), date);
        if (oh.isEmpty()) {
            return ResponseEntity.ok(new java.util.HashMap<String, Object>(){{
                put("exists", false);
            }});
        }
        var hours = oh.get();
        List<String> timeOptions = new ArrayList<>();
        LocalTime openFrom = hours.getOpenFrom();
        LocalTime openUntil = hours.getOpenUntil();
        for (LocalTime t = openFrom; !t.isAfter(openUntil); t = t.plusMinutes(15)) {
            timeOptions.add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
        }
        return ResponseEntity.ok(new java.util.HashMap<Object, Object>(){{
            put("exists", true);
            put("openFrom", String.format("%02d:%02d", openFrom.getHour(), openFrom.getMinute()));
            put("openUntil", String.format("%02d:%02d", openUntil.getHour(), openUntil.getMinute()));
            put("timeOptions", timeOptions);
        }});
    }

    @GetMapping("/api/available-vehicles")
    @ResponseBody
    public ResponseEntity<?> getAvailableVehicles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int year = appConfig.getFestivalYear();
        var vehicles = vehicleService.getAvailableVehiclesForDate(year, date);
        var result = vehicles.stream().map(v -> new java.util.HashMap<String, Object>(){{
            put("vehicleId", v.getVehicleId());
            put("label", "Bil " + v.getVehicleNo() + (v.getModel() != null ? ": " + v.getModel() : ""));
            put("reg", v.getRegistration());
            put("payload", v.getPayloadKg());
            put("cargoL", v.getCargoLengthMm());
            put("cargoW", v.getCargoWidthMm());
            put("cargoH", v.getCargoHeightMm());
            put("lifter", v.isHasPalletLifter());
            put("truck", v.isHasSackTruck());
        }}).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/driver-conflicts")
    @ResponseBody
    public ResponseEntity<?> getDriverConflicts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
            @RequestParam(required = false) Integer excludeTaskId) {
        int year = appConfig.getFestivalYear();
        OffsetDateTime reqStart = date.atTime(start).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime reqEnd   = date.atTime(end).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        java.util.Map<String, String> conflicts = new java.util.LinkedHashMap<>();
        taskService.getTasksForDay(year, date).stream()
            .filter(t -> t.getDriverUser() != null)
            .filter(t -> t.getStatus() != TaskStatus.CANCELLED)
            .filter(t -> excludeTaskId == null || t.getTaskId() != excludeTaskId)
            .filter(t -> t.getStartTs() != null && t.getEndTs() != null)
            .filter(t -> t.getStartTs().isBefore(reqEnd) && t.getEndTs().isAfter(reqStart))
            .forEach(t -> conflicts.put(
                String.valueOf(t.getDriverUser().getId()),
                t.getStartTs().toLocalTime().format(HH_MM) + "–" + t.getEndTs().toLocalTime().format(HH_MM)));
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/api/vehicles/{vehicleId}/last-driver")
    @ResponseBody
    public ResponseEntity<?> getLastDriverForVehicle(@PathVariable int vehicleId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                     @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime start) {
        OffsetDateTime beforeTs = (date != null && start != null) ? toOffsetDateTime(date, start) : null;
        var suggested = taskService.findSuggestedDriver(appConfig.getFestivalYear(), vehicleId, beforeTs);
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("driverName", suggested.map(TaskService.SuggestedDriver::driverName).orElse(""));
            put("sourceStartTime", suggested
                .map(TaskService.SuggestedDriver::sourceStartTs)
                .map(v -> v.toLocalTime().format(HH_MM))
                .orElse(""));
        }});
    }

@PostMapping("/api/{id}/move")
    @ResponseBody
    public ResponseEntity<?> moveTask(@PathVariable int id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                                      @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                                      @RequestParam(required = false) Integer vehicleId) {
        try {
            Task moved = taskService.moveTask(appConfig.getFestivalYear(), id, date, start, end, vehicleId);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("taskId", moved.getTaskId());
                put("start", start.toString());
                put("end", end.toString());
                put("vehicleId", moved.getVehicleId());
            }});
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                put("error", ex.getMessage());
            }});
        }
    }

    @PostMapping("/api/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelTaskApi(@PathVariable int id) {
        try {
            Task task = taskService.getById(appConfig.getFestivalYear(), id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
            TaskStatus next = task.getStatus() == TaskStatus.CANCELLED ? TaskStatus.ORDERED : TaskStatus.CANCELLED;
            Task updated = taskService.updateStatus(appConfig.getFestivalYear(), id, next);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("taskId", updated.getTaskId());
                put("status", updated.getStatus().name());
                put("statusLabel", updated.getStatus().danishLabel());
            }});
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                put("error", ex.getMessage());
            }});
        }
    }

    @PostMapping("/api/{id}/duplicate")
    @ResponseBody
    public ResponseEntity<?> duplicateTask(@PathVariable int id) {
        try {
            Task duplicated = taskService.duplicateWithoutVehicle(appConfig.getFestivalYear(), id);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("taskId", duplicated.getTaskId());
                put("vehicleId", duplicated.getVehicleId());
                put("status", duplicated.getStatus().name());
            }});
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                put("error", ex.getMessage());
            }});
        }
    }

    private void populateForm(Task task, LocalDate date, String returnTo, Model model) throws Exception {
        // Determine the reference date for time options
        LocalDate refDate = date;
        if (refDate == null && task.getStartTs() != null) {
            refDate = task.getStartTs().toLocalDate();
        }
        if (refDate == null) {
            refDate = LocalDate.now();
        }

        // Load opening hours and generate 5-minute time options
        List<String> timeOptions = new ArrayList<>();
        var oh = openingHoursRepository.findByFestivalYearAndFestivalDate(appConfig.getFestivalYear(), refDate);
        LocalTime openFrom  = oh.map(h -> h.getOpenFrom()).orElse(LocalTime.of(8, 0));
        LocalTime openUntil = oh.map(h -> h.getOpenUntil()).orElse(null);
        if (oh.isPresent()) {
            for (LocalTime t = openFrom; !t.isAfter(openUntil); t = t.plusMinutes(15)) {
                timeOptions.add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
            }
        }

        // Pre-select default start time for new tasks
        if (date != null && task.getTaskId() == 0 && task.getStartTs() == null) {
            LocalTime defaultTime;
            if (date.equals(LocalDate.now())) {
                // Round up to next 5-minute slot
                LocalTime now = LocalTime.now(ZoneId.systemDefault());
                int totalMins = now.getHour() * 60 + now.getMinute();
                int nextSlotMins = ((totalMins / 5) + 1) * 5;
                defaultTime = LocalTime.of((nextSlotMins / 60) % 24, nextSlotMins % 60);
                // Clamp within opening hours
                if (defaultTime.isBefore(openFrom)) defaultTime = openFrom;
                if (openUntil != null && defaultTime.isAfter(openUntil)) defaultTime = openFrom;
            } else {
                defaultTime = openFrom;
            }
            task.setStartTs(date.atTime(defaultTime).atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }

        List<User> allDrivers = userRepository.findByRoleOrderByUsernameAsc(User.Role.DRIVER);
        List<User> todayDrivers = taskService.getDriverUsersForDay(appConfig.getFestivalYear(), LocalDate.now());
        List<Long> todayDriverIds = todayDrivers.stream().map(User::getId).toList();
        List<User> otherDrivers = allDrivers.stream()
                .filter(d -> !todayDriverIds.contains(d.getId()))
                .toList();

        model.addAttribute("task", task);
        model.addAttribute("returnTo", returnTo != null ? returnTo : "fleet");
        model.addAttribute("timeOptions", timeOptions);
        model.addAttribute("vehicles", vehicleService.getAvailableVehiclesForDate(appConfig.getFestivalYear(), refDate));
        model.addAttribute("todayDrivers", todayDrivers);
        model.addAttribute("otherDrivers", otherDrivers);
        model.addAttribute("statuses", editableStatuses());
        model.addAttribute("divisions", divisionTeamData.getDivisions());
        model.addAttribute("divisionTeamsJson", objectMapper.writeValueAsString(divisionTeamData.getAllData()));
    }

    private List<TaskStatus> editableStatuses() {
        return List.of(TaskStatus.ORDERED, TaskStatus.STARTED, TaskStatus.DONE);
    }

    private boolean isAllowedStatusTransition(TaskStatus from, TaskStatus to) {
        if (to == TaskStatus.CANCELLED) {
            return false;
        }
        return switch (from) {
            case ORDERED -> to == TaskStatus.STARTED;
            case STARTED -> to == TaskStatus.ORDERED || to == TaskStatus.DONE;
            case DONE -> to == TaskStatus.ORDERED;
            case CANCELLED -> false;
        };
    }

    private String redirectToDate(LocalDate date) {
        return "redirect:/tasks" + (date != null ? "?date=" + date : "");
    }

    private String redirectToTaskDate(LocalDate date) {
        return "redirect:/tasks" + (date != null ? "?date=" + date : "");
    }

    private String redirectToTaskDate(LocalDate date, boolean showCancelled) {
        return redirectToTaskDate(date, showCancelled, false);
    }

    private String redirectToTaskDate(LocalDate date, boolean showCancelled, boolean showDone) {
        StringBuilder redirect = new StringBuilder("redirect:/tasks");
        List<String> params = new ArrayList<>();
        if (date != null) params.add("date=" + date);
        if (showCancelled) params.add("showCancelled=true");
        if (showDone) params.add("showDone=true");
        if (!params.isEmpty()) redirect.append('?').append(String.join("&", params));
        return redirect.toString();
    }

    private String redirectToFleetDate(LocalDate date) {
        return "redirect:/fleet" + (date != null ? "?date=" + date : "");
    }

    private String redirectToSearch(String q) {
        if (q == null || q.isBlank()) return "redirect:/tasks/search";
        return "redirect:/tasks/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
    }

    private LocalDate resolveRedirectDate(int taskId, LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }
        return taskService.getById(appConfig.getFestivalYear(), taskId)
            .map(Task::getStartTs)
            .filter(Objects::nonNull)
            .map(v -> v.toLocalDate())
            .orElse(LocalDate.now());
    }

    private void validateTimeRange(Task task, BindingResult result) {
        if (task.getStartTs() == null) {
            result.rejectValue("startTs", "task.startTs.required", "Dato / Start og Tid (fra) er obligatorisk.");
        }
        if (task.getEndTs() == null) {
            result.rejectValue("endTs", "task.endTs.required", "Tid (til) er obligatorisk.");
        }
        if (task.getStartTs() == null || task.getEndTs() == null) {
            return;
        }
        if (!task.getEndTs().isAfter(task.getStartTs())) {
            result.rejectValue("endTs", "task.endTs.beforeStart", "Sluttid skal være efter starttid.");
        }
    }

    private void applyNewTaskPrefill(Task task,
                                     LocalDate date,
                                     LocalTime start,
                                     Integer vehicleId) {
        if (vehicleId != null) {
            task.setVehicleId(vehicleId);
        }
        if (date == null || start == null) {
            return;
        }
        task.setStartTs(toOffsetDateTime(date, start));
    }

    private java.util.Optional<TaskService.SuggestedDriver> applySuggestedDriverName(Task task) {
        if (task.getVehicleId() == null) {
            return java.util.Optional.empty();
        }
        if (task.getDriverName() != null && !task.getDriverName().trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        var suggested = taskService.findSuggestedDriver(appConfig.getFestivalYear(), task.getVehicleId(), task.getStartTs());
        suggested.map(TaskService.SuggestedDriver::driverName).ifPresent(task::setDriverName);
        return suggested;
    }

    private boolean hasStartRequirements(Task task) {
        return task.getVehicleId() != null && task.getDriverUser() != null;
    }

    private java.time.OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time) {
        try {
            return date.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeException ex) {
            return null;
        }
    }

    private boolean isDriver(Authentication auth) {
        if (auth == null) return false;
        var auths = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return auths.contains("ROLE_DRIVER") && !auths.contains("ROLE_ADMIN");
    }
}
