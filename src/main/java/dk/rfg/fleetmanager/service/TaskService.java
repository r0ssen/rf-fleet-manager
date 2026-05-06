package dk.rfg.fleetmanager.service;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskId;
import dk.rfg.fleetmanager.entity.TaskStatus;
import dk.rfg.fleetmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository repo;
    public TaskService(TaskRepository repo) { this.repo = repo; }

    public List<Task> getTasksForDay(int year, LocalDate date) { return repo.findByDay(year, date); }
    public Optional<Task> getById(int year, int id) { return repo.findById(new TaskId(year, id)); }

    @Transactional public Task save(Task task) { return repo.save(task); }

    @Transactional
    public Task updateStatus(int year, int id, TaskStatus status) {
        Task t = repo.findById(new TaskId(year, id))
                     .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        t.setStatus(status);
        return repo.save(t);
    }

    @Transactional
    public Task moveTask(int year, int id, LocalDate date, LocalTime start, LocalTime end, Integer vehicleId) {
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("Dato, start og slut er obligatoriske.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Sluttid skal være efter starttid.");
        }

        Task task = repo.findById(new TaskId(year, id))
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Annullerede kørsler kan ikke flyttes. Genåbn først.");
        }

        task.setStartTs(date.atTime(start).atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setEndTs(date.atTime(end).atZone(ZoneId.systemDefault()).toOffsetDateTime());
        task.setVehicleId(vehicleId);
        return repo.save(task);
    }

    @Transactional
    public Task duplicateWithoutVehicle(int year, int id) {
        Task source = repo.findById(new TaskId(year, id))
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        Task copy = new Task();
        copy.setFestivalYear(year);
        copy.setStartTs(source.getStartTs());
        copy.setEndTs(source.getEndTs());
        copy.setStartPoint(source.getStartPoint());
        copy.setEndPoint(source.getEndPoint());
        copy.setViaPoint(source.getViaPoint());
        copy.setDivision(source.getDivision());
        copy.setTeam(source.getTeam());
        copy.setBookerName(source.getBookerName());
        copy.setBookerPhone(source.getBookerPhone());
        copy.setContactName(source.getContactName());
        copy.setContactPhone(source.getContactPhone());
        copy.setDescription(source.getDescription());
        copy.setVehicle(null);
        copy.setVehicleId(null);
        copy.setDriverName(null);
        copy.setReceivedBy(null);
        copy.setStatus(TaskStatus.ORDERED);

        return repo.save(copy);
    }

    @Transactional
    public void delete(int year, int id) {
        Task task = repo.findById(new TaskId(year, id))
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        task.setStatus(TaskStatus.CANCELLED);
        repo.save(task);
    }
}
