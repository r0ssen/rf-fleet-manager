package dk.rfg.fleetmanager.service;

import dk.rfg.fleetmanager.entity.OpeningHours;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskStatus;
import dk.rfg.fleetmanager.entity.Vehicle;
import dk.rfg.fleetmanager.repository.OpeningHoursRepository;
import dk.rfg.fleetmanager.repository.TaskRepository;
import dk.rfg.fleetmanager.repository.VehicleRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FleetGridService {

    private static final int LANE_HEIGHT_PX = 70;
    private static final int BLOCK_HEIGHT_PX = 62;
    private static final double COMPACT_BLOCK_MAX_WIDTH_PERCENT = 12.0;
    private static final ZoneId DISPLAY_ZONE = ZoneId.systemDefault();

    private final TaskRepository taskRepository;
    private final VehicleRepository vehicleRepository;
    private final OpeningHoursRepository openingHoursRepository;

    public FleetGridService(TaskRepository taskRepository,
                            VehicleRepository vehicleRepository,
                            OpeningHoursRepository openingHoursRepository) {
        this.taskRepository        = taskRepository;
        this.vehicleRepository     = vehicleRepository;
        this.openingHoursRepository = openingHoursRepository;
    }

    public record GridData(
        List<TimelineRow> rows,
        List<TimeMarker> timeMarkers,
        LocalTime openFrom,
        LocalTime openUntil,
        boolean hoursFromDb
    ) {}

    public record TimelineRow(
        String label,
        String subLabel,
        String editUrl,
        Integer vehicleId,
        List<TaskBlock> taskBlocks,
        int rowHeightPx,
        boolean unassigned
    ) {}

    public record TimeMarker(
        String label,
        boolean labelVisible,
        boolean hourBoundary,
        String style
    ) {}

    public record TaskBlock(
        int taskId,
        Integer vehicleId,
        String startTime,
        String endTime,
        String bookerName,
        String routeLabel,
        String timeLabel,
        String statusClass,
        String statusLabel,
        String title,
        String style,
        boolean darkText,
        boolean overlapping,
        boolean compactText,
        String driverLabel
    ) {}

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GridData buildGrid(int festivalYear, LocalDate date) {
        return buildGrid(festivalYear, date, false);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GridData buildGrid(int festivalYear, LocalDate date, boolean hideCancelled) {

        // ── Opening hours — DB only; missing hours means header-only grid ─
        OpeningHours oh = loadOpeningHours(festivalYear, date);

        LocalTime openFrom  = oh != null ? oh.getOpenFrom()  : null;
        LocalTime openUntil = oh != null ? oh.getOpenUntil() : null;
        boolean hoursFromDb = oh != null;

        // ── Vehicles are still shown in the header even if hours are missing ─
        List<Vehicle> vehicles;
        try {
            vehicles = vehicleRepository.findAvailableForDate(festivalYear, date);
        } catch (DataAccessException ex) {
            vehicles = List.of();
        }

        vehicles = vehicles.stream()
            .sorted(Comparator
                .comparingInt((Vehicle vehicle) -> parseVehicleNo(vehicle.getVehicleNo()))
                .thenComparing(vehicle -> Optional.ofNullable(vehicle.getVehicleNo()).orElse(""), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Vehicle::getVehicleId)
            )
            .toList();

        if (!hoursFromDb) {
            return new GridData(buildEmptyRows(vehicles), List.of(), null, null, false);
        }

        OffsetDateTime dayStart = date.atStartOfDay(DISPLAY_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = dayStart.plusDays(1);
        OffsetDateTime openingStart = date.atTime(openFrom).atZone(DISPLAY_ZONE).toOffsetDateTime();
        OffsetDateTime openingEnd = date.atTime(openUntil).atZone(DISPLAY_ZONE).toOffsetDateTime();
        long totalMinutes = Math.max(1, Duration.between(openFrom, openUntil).toMinutes());

        List<Task> tasks;
        try {
            tasks = taskRepository.findOverlappingDay(festivalYear, dayStart, dayEnd);
        } catch (DataAccessException ex) {
            tasks = List.of();
        }
        if (hideCancelled) {
            tasks = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .toList();
        }

        Map<Integer, List<Task>> tasksByVehicle = tasks.stream()
            .filter(task -> task.getVehicleId() != null)
            .collect(Collectors.groupingBy(Task::getVehicleId, LinkedHashMap::new, Collectors.toList()));

        List<TimelineRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            rows.add(buildRow(
                vehicle.displayLabel(),
                vehicle.getRegistration(),
                "/vehicles/" + vehicle.getVehicleId() + "/edit?returnTo=fleet",
                vehicle.getVehicleId(),
                tasksByVehicle.getOrDefault(vehicle.getVehicleId(), List.of()),
                openingStart,
                openingEnd,
                totalMinutes,
                false
            ));
        }

        List<Task> unassignedTasks = tasks.stream()
            .filter(task -> task.getVehicleId() == null)
            .toList();
        if (!unassignedTasks.isEmpty()) {
            rows.add(0, buildRow(
                "Ikke tildelt",
                "Kørsler uden bil",
                null,
                null,
                unassignedTasks,
                openingStart,
                openingEnd,
                totalMinutes,
                true
            ));
        }

        return new GridData(rows, buildTimeMarkers(openFrom, openUntil, totalMinutes), openFrom, openUntil, true);
    }

    private List<TimelineRow> buildEmptyRows(List<Vehicle> vehicles) {
        return vehicles.stream()
            .map(vehicle -> new TimelineRow(vehicle.displayLabel(), vehicle.getRegistration(), "/vehicles/" + vehicle.getVehicleId() + "/edit?returnTo=fleet", vehicle.getVehicleId(), List.of(), 56, false))
            .toList();
    }

    private OpeningHours loadOpeningHours(int festivalYear, LocalDate date) {
        try {
            return openingHoursRepository
                .findByFestivalYearAndFestivalDate(festivalYear, date)
                .orElse(null);
        } catch (DataAccessException ex) {
            // In dev, allow the grid page to render even if schema is not initialized yet.
            return null;
        }
    }

    private TimelineRow buildRow(String label,
                                 String subLabel,
                                 String editUrl,
                                 Integer vehicleId,
                                 List<Task> tasks,
                                 OffsetDateTime openingStart,
                                 OffsetDateTime openingEnd,
                                 long totalMinutes,
                                 boolean unassigned) {
        List<ScheduledTask> scheduledTasks = tasks.stream()
            .map(task -> clipTask(task, openingStart, openingEnd))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ScheduledTask::viewStart).thenComparing(scheduled -> scheduled.task().getTaskId()))
            .toList();

        Set<Integer> overlappingTaskIds = unassigned
            ? Set.of()
            : findOverlappingTaskIds(
                scheduledTasks.stream()
                    .filter(scheduled -> scheduled.task().getStatus() != TaskStatus.CANCELLED)
                    .toList()
            );

        // First pass: assign lanes so we know the total lane count before computing heights.
        List<OffsetDateTime> laneEnds = new ArrayList<>();
        List<Integer> laneIndices = new ArrayList<>();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            laneIndices.add(assignLane(laneEnds, scheduledTask.viewStart(), scheduledTask.viewEnd()));
        }

        int numLanes = laneEnds.size();
        int rowHeightPx = Math.max(BLOCK_HEIGHT_PX + 12, numLanes * LANE_HEIGHT_PX + 8);

        // Second pass: build task blocks with heights that account for the full row size.
        List<TaskBlock> taskBlocks = new ArrayList<>();
        for (int i = 0; i < scheduledTasks.size(); i++) {
            ScheduledTask scheduledTask = scheduledTasks.get(i);
            taskBlocks.add(toTaskBlock(
                scheduledTask,
                openingStart,
                totalMinutes,
                laneIndices.get(i),
                overlappingTaskIds.contains(scheduledTask.task().getTaskId()),
                rowHeightPx,
                numLanes
            ));
        }

        return new TimelineRow(label, subLabel, editUrl, vehicleId, taskBlocks, rowHeightPx, unassigned);
    }

    private ScheduledTask clipTask(Task task, OffsetDateTime openingStart, OffsetDateTime openingEnd) {
        if (task.getStartTs() == null) {
            return null;
        }

        OffsetDateTime actualEnd = task.getEndTs() != null ? task.getEndTs() : task.getStartTs().plusMinutes(15);
        if (!actualEnd.isAfter(task.getStartTs())) {
            actualEnd = task.getStartTs().plusMinutes(15);
        }

        OffsetDateTime viewStart = task.getStartTs().isAfter(openingStart) ? task.getStartTs() : openingStart;
        OffsetDateTime viewEnd = actualEnd.isBefore(openingEnd) ? actualEnd : openingEnd;
        if (!viewEnd.isAfter(viewStart)) {
            return null;
        }
        return new ScheduledTask(task, viewStart, viewEnd, actualEnd);
    }

    private int assignLane(List<OffsetDateTime> laneEnds, OffsetDateTime viewStart, OffsetDateTime viewEnd) {
        for (int i = 0; i < laneEnds.size(); i++) {
            if (!viewStart.isBefore(laneEnds.get(i))) {
                laneEnds.set(i, viewEnd);
                return i;
            }
        }
        laneEnds.add(viewEnd);
        return laneEnds.size() - 1;
    }

    private TaskBlock toTaskBlock(ScheduledTask scheduledTask,
                                  OffsetDateTime openingStart,
                                  long totalMinutes,
                                  int laneIndex,
                                  boolean overlapping,
                                  int rowHeightPx,
                                  int numLanes) {
        Task task = scheduledTask.task();
        long offsetMinutes = Duration.between(openingStart, scheduledTask.viewStart()).toMinutes();
        long durationMinutes = Math.max(1, Duration.between(scheduledTask.viewStart(), scheduledTask.viewEnd()).toMinutes());

        double leftPercent = (offsetMinutes * 100.0) / totalMinutes;
        double widthPercent = (durationMinutes * 100.0) / totalMinutes;
        int topPx = laneIndex * LANE_HEIGHT_PX + 4;
        int heightPx = (!overlapping && numLanes > 1) ? rowHeightPx - 8 : BLOCK_HEIGHT_PX;
        String style = "left:" + pct(leftPercent)
            + ";width:" + pct(widthPercent)
            + ";top:" + topPx + "px;height:" + heightPx + "px;";
        boolean compactText = widthPercent <= COMPACT_BLOCK_MAX_WIDTH_PERCENT;

        String bookerName = task.getBookerName() == null || task.getBookerName().isBlank()
            ? "(ingen bestiller)"
            : task.getBookerName().trim();
        String routeLabel = buildRouteLabel(task);
        String timeLabel = formatTime(task.getStartTs()) + " – " + formatTime(scheduledTask.actualEnd());
        String driverLabel = task.getDriverUser() != null ? task.getDriverUser().getUsername()
            : (task.getDriverName() != null && !task.getDriverName().isBlank() ? task.getDriverName().trim() : null);
        String title = bookerName + " | " + timeLabel + " | " + task.getStatus().danishLabel()
            + (routeLabel.isBlank() ? "" : " | " + routeLabel)
            + (task.getDescription() == null || task.getDescription().isBlank() ? "" : " | " + task.getDescription().trim())
            + (overlapping ? " | OVERLAP MED ANDEN KORSEL" : "");

        return new TaskBlock(
            task.getTaskId(),
            task.getVehicleId(),
            formatTime(task.getStartTs()),
            formatTime(scheduledTask.actualEnd()),
            bookerName,
            routeLabel,
            timeLabel,
            task.getStatus().name().toLowerCase(Locale.ROOT),
            task.getStatus().danishLabel(),
            title,
            style,
            task.getStatus() == TaskStatus.STARTED,
            overlapping,
            compactText,
            driverLabel
        );
    }

    private Set<Integer> findOverlappingTaskIds(List<ScheduledTask> scheduledTasks) {
        Set<Integer> overlappingTaskIds = new HashSet<>();
        for (int i = 0; i < scheduledTasks.size(); i++) {
            ScheduledTask a = scheduledTasks.get(i);
            for (int j = i + 1; j < scheduledTasks.size(); j++) {
                ScheduledTask b = scheduledTasks.get(j);
                if (intervalsOverlap(a, b)) {
                    overlappingTaskIds.add(a.task().getTaskId());
                    overlappingTaskIds.add(b.task().getTaskId());
                }
            }
        }
        return overlappingTaskIds;
    }

    private boolean intervalsOverlap(ScheduledTask a, ScheduledTask b) {
        return a.viewStart().isBefore(b.viewEnd()) && b.viewStart().isBefore(a.viewEnd());
    }

    private List<TimeMarker> buildTimeMarkers(LocalTime openFrom, LocalTime openUntil, long totalMinutes) {
        List<TimeMarker> markers = new ArrayList<>();
        for (LocalTime time = openFrom; !time.isAfter(openUntil); time = time.plusMinutes(15)) {
            long offsetMinutes = Duration.between(openFrom, time).toMinutes();
            boolean labelVisible = time.equals(openFrom) || time.equals(openUntil) || time.getMinute() == 0;
            markers.add(new TimeMarker(
                time.toString(),
                labelVisible,
                time.getMinute() == 0,
                "left:" + pct((offsetMinutes * 100.0) / totalMinutes) + ";"
            ));
        }
        return markers;
    }

    private String buildRouteLabel(Task task) {
        String start = task.getStartPoint() != null ? task.getStartPoint().trim() : "";
        String end = task.getEndPoint() != null ? task.getEndPoint().trim() : "";
        if (start.isBlank() && end.isBlank()) {
            return "";
        }
        if (end.isBlank()) {
            return start;
        }
        if (start.isBlank()) {
            return end;
        }
        String via = task.getViaPoint() != null && !task.getViaPoint().isBlank()
            ? " → " + task.getViaPoint().replace("|", " → ")
            : "";
        return start + via + " → " + end;
    }

    private String formatTime(OffsetDateTime value) {
        if (value == null) {
            return "?";
        }
        return value.toLocalTime().withSecond(0).withNano(0).toString();
    }


    private String pct(double value) {
        return String.format(Locale.ROOT, "%.4f%%", value);
    }

    private int parseVehicleNo(String vehicleNo) {
        if (vehicleNo == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(vehicleNo.trim());
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private record ScheduledTask(Task task,
                                 OffsetDateTime viewStart,
                                 OffsetDateTime viewEnd,
                                 OffsetDateTime actualEnd) {
    }
}
