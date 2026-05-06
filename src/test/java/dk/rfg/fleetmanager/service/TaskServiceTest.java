package dk.rfg.fleetmanager.service;

import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskId;
import dk.rfg.fleetmanager.entity.TaskStatus;
import dk.rfg.fleetmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskService service = new TaskService(taskRepository);

    @Test
    void duplicateWithoutVehicleCopiesTaskAndClearsAssignmentState() {
        Task source = sourceTask();
        when(taskRepository.findById(new TaskId(2026, 17))).thenReturn(Optional.of(source));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task copy = service.duplicateWithoutVehicle(2026, 17);

        assertThat(copy).isNotSameAs(source);
        assertThat(copy.getFestivalYear()).isEqualTo(2026);
        assertThat(copy.getTaskId()).isZero();
        assertThat(copy.getStartTs()).isEqualTo(source.getStartTs());
        assertThat(copy.getEndTs()).isEqualTo(source.getEndTs());
        assertThat(copy.getStartPoint()).isEqualTo(source.getStartPoint());
        assertThat(copy.getEndPoint()).isEqualTo(source.getEndPoint());
        assertThat(copy.getViaPoint()).isEqualTo(source.getViaPoint());
        assertThat(copy.getDivision()).isEqualTo(source.getDivision());
        assertThat(copy.getTeam()).isEqualTo(source.getTeam());
        assertThat(copy.getBookerName()).isEqualTo(source.getBookerName());
        assertThat(copy.getBookerPhone()).isEqualTo(source.getBookerPhone());
        assertThat(copy.getContactName()).isEqualTo(source.getContactName());
        assertThat(copy.getContactPhone()).isEqualTo(source.getContactPhone());
        assertThat(copy.getDescription()).isEqualTo(source.getDescription());
        assertThat(copy.getVehicle()).isNull();
        assertThat(copy.getVehicleId()).isNull();
        assertThat(copy.getDriverName()).isNull();
        assertThat(copy.getReceivedBy()).isNull();
        assertThat(copy.getStatus()).isEqualTo(TaskStatus.ORDERED);
        assertThat(copy.getVersion()).isZero();
        assertThat(copy.getCreatedAt()).isNull();
    }

    @Test
    void duplicateWithoutVehicleFailsWhenTaskDoesNotExist() {
        when(taskRepository.findById(new TaskId(2026, 404))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.duplicateWithoutVehicle(2026, 404))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Task not found: 404");
    }

    private Task sourceTask() {
        LocalDate date = LocalDate.of(2026, 5, 5);
        Task task = new Task();
        task.setFestivalYear(2026);
        task.setTaskId(17);
        task.setStartTs(date.atTime(LocalTime.of(10, 0)).atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setEndTs(date.atTime(LocalTime.of(11, 15)).atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setStartPoint("Lager");
        task.setEndPoint("Scene");
        task.setViaPoint("Depot");
        task.setDivision("Logistik");
        task.setTeam("Transport");
        task.setBookerName("Mads");
        task.setBookerPhone("12345678");
        task.setContactName("Pia");
        task.setContactPhone("87654321");
        task.setVehicleId(3);
        task.setDriverName("Søren");
        task.setDescription("Højtalere");
        task.setStatus(TaskStatus.CANCELLED);
        task.setReceivedBy("Dispatch");
        task.setVersion(5L);
        task.setCreatedAt(date.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        return task;
    }
}

