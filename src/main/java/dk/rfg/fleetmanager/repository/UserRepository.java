package dk.rfg.fleetmanager.repository;

import dk.rfg.fleetmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRoleOrderByUsernameAsc(User.Role role);
}
