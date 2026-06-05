package dk.rfg.fleetmanager.config;

import dk.rfg.fleetmanager.entity.User;
import dk.rfg.fleetmanager.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepository.count() == 0) {
                userRepository.save(new User("admin", passwordEncoder.encode("admin"), User.Role.ADMIN));
                userRepository.save(new User("agent", passwordEncoder.encode("agent"), User.Role.AGENT));
            }
        } catch (Exception ignored) {
            // users table may not exist in isolated integration tests that manage their own schema
        }
    }
}
