package dk.rfg.fleetmanager.repository;

import dk.rfg.fleetmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role ORDER BY u.username ASC")
    List<User> findByRoleOrderByUsernameAsc(@Param("role") User.Role role);
}
