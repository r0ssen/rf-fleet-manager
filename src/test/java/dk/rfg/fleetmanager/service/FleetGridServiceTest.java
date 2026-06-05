package dk.rfg.fleetmanager.service;

import dk.rfg.fleetmanager.entity.OpeningHours;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskStatus;
import dk.rfg.fleetmanager.entity.Vehicle;
import dk.rfg.fleetmanager.repository.OpeningHoursRepository;
import dk.rfg.fleetmanager.repository.TaskRepository;
import dk.rfg.fleetmanager.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetGridServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final OpeningHoursRepository openingHoursRepository = mock(OpeningHoursRepository.class);
    private final FleetGridService service = new FleetGridService(taskRepository, vehicleRepository, openingHoursRepository);

    @Test
    void buildGridCreatesTimelineBlocksAndUnassignedRow() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(openingHoursRepository.findByFestivalYearAndFestivalDate(2026, date)).thenReturn(Optional.of(openingHours(date, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(vehicleRepository.findAvailableForDate(2026, date)).thenReturn(List.of(vehicle(1, "1"), vehicle(2, "2")));
        when(taskRepository.findOverlappingDay(eq(2026), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(
            task(11, 1, date.atTime(8, 15), date.atTime(9, 0), "Alice", TaskStatus.ORDERED),
            task(12, null, date.atTime(10, 0), date.atTime(10, 30), "Bob", TaskStatus.DONE)
        ));

        FleetGridService.GridData grid = service.buildGrid(2026, date);

        assertThat(grid.hoursFromDb()).isTrue();
        assertThat(grid.rows()).hasSize(3);
        // Unassigned row is now first
        assertThat(grid.rows().getFirst().label()).isEqualTo("Ikke tildelt");
        assertThat(grid.rows().getFirst().taskBlocks().getFirst().bookerName()).isEqualTo("Bob");
        assertThat(grid.rows().getFirst().taskBlocks().getFirst().overlapping()).isFalse();
        // Bil 1 is second
        assertThat(grid.rows().get(1).label()).isEqualTo("Bil 1");
        assertThat(grid.rows().get(1).taskBlocks()).hasSize(1);
        assertThat(grid.rows().get(1).taskBlocks().getFirst().bookerName()).isEqualTo("Alice");
        assertThat(grid.rows().get(1).taskBlocks().getFirst().style()).contains("left:6.2500%", "width:18.7500%", "top:4px");
        assertThat(grid.rows().get(1).taskBlocks().getFirst().overlapping()).isFalse();
        assertThat(grid.rows().get(1).taskBlocks().getFirst().compactText()).isFalse();
    }

    @Test
    void buildGridStacksOverlappingTasksInSeparateLanes() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(openingHoursRepository.findByFestivalYearAndFestivalDate(2026, date)).thenReturn(Optional.of(openingHours(date, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(vehicleRepository.findAvailableForDate(2026, date)).thenReturn(List.of(vehicle(1, "1")));
        when(taskRepository.findOverlappingDay(eq(2026), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(
            task(21, 1, date.atTime(8, 0), date.atTime(9, 0), "Alpha", TaskStatus.ORDERED),
            task(22, 1, date.atTime(8, 30), date.atTime(9, 15), "Bravo", TaskStatus.STARTED)
        ));

        FleetGridService.GridData grid = service.buildGrid(2026, date);

        assertThat(grid.rows()).hasSize(1);
        assertThat(grid.rows().getFirst().taskBlocks()).hasSize(2);
        assertThat(grid.rows().getFirst().rowHeightPx()).isGreaterThan(56);
        assertThat(grid.rows().getFirst().taskBlocks().get(0).style()).contains("top:4px");
        assertThat(grid.rows().getFirst().taskBlocks().get(1).style()).contains("top:74px");
        assertThat(grid.rows().getFirst().taskBlocks().get(0).overlapping()).isTrue();
        assertThat(grid.rows().getFirst().taskBlocks().get(1).overlapping()).isTrue();
    }

    @Test
    void buildGridIgnoresCancelledTasksWhenMarkingOverlaps() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(openingHoursRepository.findByFestivalYearAndFestivalDate(2026, date)).thenReturn(Optional.of(openingHours(date, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(vehicleRepository.findAvailableForDate(2026, date)).thenReturn(List.of(vehicle(1, "1")));
        when(taskRepository.findOverlappingDay(eq(2026), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(
            task(31, 1, date.atTime(8, 0), date.atTime(9, 0), "Active", TaskStatus.ORDERED),
            task(32, 1, date.atTime(8, 15), date.atTime(8, 45), "Cancelled", TaskStatus.CANCELLED)
        ));

        FleetGridService.GridData grid = service.buildGrid(2026, date);

        assertThat(grid.rows()).hasSize(1);
        assertThat(grid.rows().getFirst().taskBlocks()).hasSize(2);
        assertThat(grid.rows().getFirst().taskBlocks().get(0).overlapping()).isFalse();
        assertThat(grid.rows().getFirst().taskBlocks().get(1).overlapping()).isFalse();
    }

    @Test
    void buildGridMarksVeryShortBlocksAsCompactText() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(openingHoursRepository.findByFestivalYearAndFestivalDate(2026, date)).thenReturn(Optional.of(openingHours(date, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(vehicleRepository.findAvailableForDate(2026, date)).thenReturn(List.of(vehicle(1, "1")));
        when(taskRepository.findOverlappingDay(eq(2026), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(
            task(41, 1, date.atTime(8, 0), date.atTime(8, 15), "Tiny", TaskStatus.ORDERED)
        ));

        FleetGridService.GridData grid = service.buildGrid(2026, date);

        assertThat(grid.rows()).hasSize(1);
        assertThat(grid.rows().getFirst().taskBlocks()).hasSize(1);
        assertThat(grid.rows().getFirst().taskBlocks().getFirst().compactText()).isTrue();
    }

    @Test
    void buildGridDoesNotMarkUnassignedTasksAsOverlapping() {
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(openingHoursRepository.findByFestivalYearAndFestivalDate(2026, date)).thenReturn(Optional.of(openingHours(date, LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(vehicleRepository.findAvailableForDate(2026, date)).thenReturn(List.of(vehicle(1, "1")));
        when(taskRepository.findOverlappingDay(eq(2026), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(
            task(51, null, date.atTime(8, 0), date.atTime(9, 0), "Alpha", TaskStatus.ORDERED),
            task(52, null, date.atTime(8, 15), date.atTime(9, 15), "Bravo", TaskStatus.STARTED)
        ));

        FleetGridService.GridData grid = service.buildGrid(2026, date);

        assertThat(grid.rows()).hasSize(2);
        // Unassigned row is now first
        assertThat(grid.rows().getFirst().label()).isEqualTo("Ikke tildelt");
        assertThat(grid.rows().getFirst().taskBlocks()).hasSize(2);
        assertThat(grid.rows().getFirst().taskBlocks()).allSatisfy(taskBlock ->
            assertThat(taskBlock.overlapping()).isFalse()
        );
    }

    private OpeningHours openingHours(LocalDate date, LocalTime from, LocalTime until) {
        OpeningHours openingHours = new OpeningHours();
        openingHours.setFestivalYear(2026);
        openingHours.setFestivalDate(date);
        openingHours.setOpenFrom(from);
        openingHours.setOpenUntil(until);
        return openingHours;
    }

    private Vehicle vehicle(int id, String vehicleNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setFestivalYear(2026);
        vehicle.setVehicleId(id);
        vehicle.setVehicleNo(vehicleNo);
        vehicle.setRegistration("AB" + id);
        return vehicle;
    }

    private Task task(int id, Integer vehicleId, java.time.LocalDateTime start, java.time.LocalDateTime end, String bookerName, TaskStatus status) {
        Task task = new Task();
        task.setFestivalYear(2026);
        task.setTaskId(id);
        task.setVehicleId(vehicleId);
        task.setStartTs(start.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setEndTs(end.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setBookerName(bookerName);
        task.setStatus(status);
        task.setStartPoint("Lager");
        task.setEndPoint("Scene");
        return task;
    }
}


