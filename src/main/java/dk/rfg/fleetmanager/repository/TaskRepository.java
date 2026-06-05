package dk.rfg.fleetmanager.repository;
import dk.rfg.fleetmanager.entity.Task;
import dk.rfg.fleetmanager.entity.TaskId;
import dk.rfg.fleetmanager.entity.TaskStatus;
import org.springframework.data.domain.Pageable;
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

    @Query("""
           SELECT t.driverName FROM Task t
           WHERE t.festivalYear = :year
             AND t.vehicleId = :vehicleId
             AND t.status <> :cancelled
             AND t.driverName IS NOT NULL
             AND TRIM(t.driverName) <> ''
             AND CAST(t.startTs AS localdate) = :date
             AND t.startTs < :beforeTs
           ORDER BY t.startTs DESC, t.taskId DESC
           """)
    List<String> findRecentDriverNamesForDay(@Param("year") int year,
                                             @Param("vehicleId") int vehicleId,
                                             @Param("cancelled") TaskStatus cancelled,
                                             @Param("date") LocalDate date,
                                             @Param("beforeTs") OffsetDateTime beforeTs,
                                             Pageable pageable);

    @Query("""
           SELECT t FROM Task t
           WHERE t.festivalYear = :year
             AND t.vehicleId = :vehicleId
             AND t.status <> :cancelled
             AND t.driverName IS NOT NULL
             AND TRIM(t.driverName) <> ''
             AND CAST(t.startTs AS localdate) = :date
             AND t.startTs < :beforeTs
           ORDER BY t.startTs DESC, t.taskId DESC
           """)
    List<Task> findRecentDriverTasksForDay(@Param("year") int year,
                                           @Param("vehicleId") int vehicleId,
                                           @Param("cancelled") TaskStatus cancelled,
                                           @Param("date") LocalDate date,
                                           @Param("beforeTs") OffsetDateTime beforeTs,
                                           Pageable pageable);

    @Query("""
           SELECT t.receivedBy FROM Task t
           WHERE t.festivalYear = :year
             AND t.status <> :cancelled
             AND t.receivedBy IS NOT NULL
             AND TRIM(t.receivedBy) <> ''
           ORDER BY t.startTs DESC, t.taskId DESC
           """)
    List<String> findRecentDispatcherNames(@Param("year") int year,
                                           @Param("cancelled") TaskStatus cancelled,
                                           Pageable pageable);

    List<Task> findByFestivalYearAndStatusOrderByStartTsAsc(int year, TaskStatus status);
}
