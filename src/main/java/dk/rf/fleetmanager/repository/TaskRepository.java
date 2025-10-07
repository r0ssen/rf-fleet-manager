package dk.rf.fleetmanager.repository;

import dk.rf.fleetmanager.entity.Task;
import dk.rf.fleetmanager.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
