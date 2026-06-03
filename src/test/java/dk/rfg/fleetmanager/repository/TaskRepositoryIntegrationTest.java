package dk.rfg.fleetmanager.repository;

import dk.rfg.fleetmanager.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.datasource.url=jdbc:h2:mem:taskrepo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Transactional
class TaskRepositoryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanYearData() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vehicles (
                festival_year SMALLINT NOT NULL,
                vehicle_id INT NOT NULL,
                vehicle_no VARCHAR(20) NOT NULL,
                CONSTRAINT pk_vehicles PRIMARY KEY (festival_year, vehicle_id)
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                festival_year SMALLINT NOT NULL,
                task_id INT NOT NULL,
                start_ts TIMESTAMP WITH TIME ZONE NOT NULL,
                vehicle_id INT,
                driver_name TEXT,
                status VARCHAR(20) NOT NULL,
                CONSTRAINT pk_tasks PRIMARY KEY (festival_year, task_id)
            )
            """);
        jdbcTemplate.update("DELETE FROM tasks WHERE festival_year = ?", 2026);
        jdbcTemplate.update("DELETE FROM vehicles WHERE festival_year = ?", 2026);
    }

    @Test
    void findRecentDriverNamesForDayReturnsLatestEarlierDriverOnSameDay() {
        LocalDate day = LocalDate.of(2026, 6, 5);
        insertVehicle(2026, 1, "1");
        insertTask(2026, 100, 1, "Anne", TaskStatus.ORDERED, day, LocalTime.of(8, 0));
        insertTask(2026, 101, 1, "Bent", TaskStatus.DONE, day, LocalTime.of(9, 0));
        insertTask(2026, 102, 1, "", TaskStatus.ORDERED, day, LocalTime.of(9, 10));

        OffsetDateTime beforeTs = toOffsetDateTime(day, LocalTime.of(9, 30));

        List<String> result = taskRepository.findRecentDriverNamesForDay(
            2026,
            1,
            TaskStatus.CANCELLED,
            day,
            beforeTs,
            PageRequest.of(0, 1)
        );

        assertThat(result).containsExactly("Bent");
    }

    @Test
    void findRecentDriverNamesForDayReturnsEmptyWhenNoEarlierTaskOnSameDay() {
        LocalDate day = LocalDate.of(2026, 6, 6);
        insertVehicle(2026, 1, "1");
        insertTask(2026, 200, 1, "Chris", TaskStatus.ORDERED, day.minusDays(1), LocalTime.of(23, 0));
        insertTask(2026, 201, 1, "Dana", TaskStatus.CANCELLED, day, LocalTime.of(8, 0));

        OffsetDateTime beforeTs = toOffsetDateTime(day, LocalTime.of(9, 0));

        List<String> result = taskRepository.findRecentDriverNamesForDay(
            2026,
            1,
            TaskStatus.CANCELLED,
            day,
            beforeTs,
            PageRequest.of(0, 1)
        );

        assertThat(result).isEmpty();
    }

    private void insertVehicle(int year, int vehicleId, String vehicleNo) {
        jdbcTemplate.update(
            "INSERT INTO vehicles (festival_year, vehicle_id, vehicle_no) VALUES (?, ?, ?)",
            year,
            vehicleId,
            vehicleNo
        );
    }

    private void insertTask(int year,
                            int taskId,
                            int vehicleId,
                            String driverName,
                            TaskStatus status,
                            LocalDate date,
                            LocalTime start) {
        jdbcTemplate.update(
            "INSERT INTO tasks (festival_year, task_id, start_ts, vehicle_id, driver_name, status) VALUES (?, ?, ?, ?, ?, ?)",
            year,
            taskId,
            toOffsetDateTime(date, start),
            vehicleId,
            driverName,
            status.name()
        );
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}


