package dk.rfg.fleetmanager.repository;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskId;
import dk.rfg.fleetmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, TaskId> {
    @Query("""
           SELECT t FROM Task t LEFT JOIN FETCH t.vehicle
           WHERE t.festivalYear = :year AND CAST(t.startTs AS localdate) = :date
           ORDER BY t.startTs
           """)
    List<Task> findByDay(@Param("year") int year, @Param("date") LocalDate date);

    @Query("""
           SELECT t FROM Task t LEFT JOIN FETCH t.vehicle
           WHERE t.festivalYear = :year
             AND t.startTs < :dayEnd
             AND COALESCE(t.endTs, t.startTs) >= :dayStart
           ORDER BY t.vehicleId NULLS LAST, t.startTs, t.taskId
           """)
    List<Task> findOverlappingDay(@Param("year") int year,
                                  @Param("dayStart") OffsetDateTime dayStart,
                                  @Param("dayEnd") OffsetDateTime dayEnd);

    List<Task> findByFestivalYearAndStatusOrderByStartTsAsc(int year, TaskStatus status);
}
