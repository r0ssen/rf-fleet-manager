package dk.rf.fleetmanager.repository;

import dk.rf.fleetmanager.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
    List<TestEntity> findByFirstName(String firstName);
}
