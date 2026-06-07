package dk.rfg.fleetmanager.controller;

import dk.rfg.fleetmanager.config.AppConfig;
import dk.rfg.fleetmanager.entity.User;
import dk.rfg.fleetmanager.repository.UserRepository;
import dk.rfg.fleetmanager.service.VehicleService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleService vehicleService;
    private final AppConfig appConfig;

    public UserAdminController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               VehicleService vehicleService, AppConfig appConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.vehicleService = vehicleService;
        this.appConfig = appConfig;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        var vehicleLabels = vehicleService.getAllVehicles(appConfig.getFestivalYear()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        v -> v.getVehicleId(),
                        v -> v.displayLabel()));
        model.addAttribute("vehicleLabels", vehicleLabels);
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
        return "admin/users/form";
    }

    @PostMapping
    public String create(@ModelAttribute UserForm form, Model model, RedirectAttributes flash) {
        List<String> errors = validate(form, null);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("userForm", form);
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
            return "admin/users/form";
        }
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            model.addAttribute("errors", List.of("Brugernavnet '" + form.getUsername() + "' er allerede i brug."));
            model.addAttribute("userForm", form);
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
            return "admin/users/form";
        }
        User user = new User(form.getUsername(), passwordEncoder.encode(form.getPassword()), form.getRole());
        user.setVehicleId(form.getRole() == User.Role.DRIVER ? form.getVehicleId() : null);
        userRepository.save(user);
        flash.addFlashAttribute("successMessage", "Brugeren '" + form.getUsername() + "' er oprettet.");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bruger ikke fundet: " + id));
        UserForm form = new UserForm();
        form.setUsername(user.getUsername());
        form.setRole(user.getRole());
        form.setVehicleId(user.getVehicleId());
        model.addAttribute("userForm", form);
        model.addAttribute("userId", id);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute UserForm form, Model model, RedirectAttributes flash) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bruger ikke fundet: " + id));

        List<String> errors = validate(form, id);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("userForm", form);
            model.addAttribute("userId", id);
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("vehicles", vehicleService.getAllVehicles(appConfig.getFestivalYear()));
            return "admin/users/form";
        }
        userRepository.findByUsername(form.getUsername()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalStateException("Brugernavnet er allerede i brug.");
            }
        });

        user.setUsername(form.getUsername());
        user.setRole(form.getRole());
        user.setVehicleId(form.getRole() == User.Role.DRIVER ? form.getVehicleId() : null);
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);
        flash.addFlashAttribute("successMessage", "Brugeren '" + user.getUsername() + "' er opdateret.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bruger ikke fundet: " + id));
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN).count();
        if (user.getRole() == User.Role.ADMIN && adminCount <= 1) {
            flash.addFlashAttribute("errorMessage", "Den eneste admin-bruger kan ikke slettes.");
            return "redirect:/admin/users";
        }
        userRepository.delete(user);
        flash.addFlashAttribute("successMessage", "Brugeren '" + user.getUsername() + "' er slettet.");
        return "redirect:/admin/users";
    }

    private List<String> validate(UserForm form, Long editingId) {
        List<String> errors = new java.util.ArrayList<>();
        if (form.getUsername() == null || form.getUsername().isBlank())
            errors.add("Brugernavn er påkrævet.");
        if (editingId == null && (form.getPassword() == null || form.getPassword().isBlank()))
            errors.add("Adgangskode er påkrævet ved oprettelse.");
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 4)
                errors.add("Adgangskoden skal være mindst 4 tegn.");
            if (!form.getPassword().equals(form.getConfirmPassword()))
                errors.add("Adgangskoderne er ikke ens.");
        }
        if (form.getRole() == null)
            errors.add("Rolle er påkrævet.");
        if (form.getRole() == User.Role.DRIVER && form.getVehicleId() == null)
            errors.add("Bil er påkrævet for chauffør-rollen.");
        return errors;
    }

    public static class UserForm {
        private String username;
        private String password;
        private String confirmPassword;
        private User.Role role;
        private Integer vehicleId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public User.Role getRole() { return role; }
        public void setRole(User.Role role) { this.role = role; }
        public Integer getVehicleId() { return vehicleId; }
        public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }
    }
}
