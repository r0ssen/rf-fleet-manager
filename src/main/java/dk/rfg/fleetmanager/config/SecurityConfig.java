package dk.rfg.fleetmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/vehicles/**", "/opening-hours/**", "/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/vehicles", "/vehicles/*/edit").hasAnyRole("ADMIN", "AGENT")
                // DRIVER may only read the task list and individual task details
                .requestMatchers(HttpMethod.GET, "/tasks", "/tasks/*/edit").hasAnyRole("ADMIN", "AGENT", "DRIVER")
                .anyRequest().hasAnyRole("ADMIN", "AGENT")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            var auths = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority()).toList();
            boolean driverOnly = auths.contains("ROLE_DRIVER") && !auths.contains("ROLE_ADMIN");
            response.sendRedirect(request.getContextPath() + (driverOnly ? "/tasks" : "/fleet"));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
